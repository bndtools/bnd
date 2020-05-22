package bndtools.m2e;

import static aQute.lib.exceptions.Exceptions.unchecked;
import static aQute.lib.exceptions.FunctionWithException.asFunction;
import static aQute.lib.exceptions.FunctionWithException.asFunctionOrElse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.PopulatedRepository;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.AbstractIndexingRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.libg.glob.Glob;
import aQute.maven.api.Revision;
import aQute.maven.provider.POM;
import bndtools.central.Central;

@Component(service = {
	IMavenProjectChangedListener.class, MavenWorkspaceRepository.class, RepositoryPlugin.class
})
public class MavenWorkspaceRepository extends
	AbstractIndexingRepository<IProject, File>
	implements IMavenProjectChangedListener, MavenRunListenerHelper, PopulatedRepository, Refreshable,
	RepositoryPlugin {

	enum Kind {
		ADDED(MavenProjectChangedEvent.KIND_ADDED),
		CHANGED(MavenProjectChangedEvent.KIND_CHANGED),
		REMOVED(MavenProjectChangedEvent.KIND_REMOVED);

		Kind(int kind) {
			this.kind = kind;
		}

		public static Kind get(MavenProjectChangedEvent event) {
			for (Kind value : values()) {
				if (value.kind == event.getKind())
					return value;
			}
			throw new IllegalArgumentException(String.valueOf(event.getKind()));
		}

		private final int kind;
	}

	private final static Logger				logger	= LoggerFactory.getLogger(MavenWorkspaceRepository.class);

	private volatile Supplier<Set<String>>								list;
	private final Function<String, Collection<Requirement>>	bsnRequirements			= bsn -> Collections
		.singleton(CapReqBuilder.createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn, null)
			.buildSyntheticRequirement());
	private final BiFunction<String, Version, Collection<Requirement>>	bsnAndVersionRequirements	= (
		bsn, version) -> Collections
			.singleton(CapReqBuilder.createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn, version
				.toString())
				.buildSyntheticRequirement());
	private final Collection<Requirement>					identityRequirements	= Collections
		.singleton(ResourceUtils.createWildcardRequirement());

	public MavenWorkspaceRepository() {
		super();
	}

	@Activate
	void activate() {
		process();
		list = memoize(this::list0);
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		File promise = get(bsn, version);
		if (promise == null) {
			if (listeners != null) {
				for (DownloadListener dl : listeners) {
					try {
						dl.failure(null, "bundle " + bsn + ":" + version + " does not exist in this workspace");
					} catch (Exception e) {
						logger.warn("updating listener has error", e);
					}
				}
			}
			return null;
		}
		if (listeners != null) {
			for (DownloadListener dl : listeners) {
				try {
					dl.success(promise);
				} catch (Exception e) {
					logger.warn("updating listener has error", e);
				}
			}
		}
		return promise;
	}

	@Override
	public String getIcon() {
		return "workspacerepo";
	}

	@Override
	public String getLocation() {
		Location location = Platform.getInstanceLocation();

		if (location != null) {
			return location.getURL()
				.toString();
		}

		return null;
	}

	@Override
	public String getName() {
		return "Maven Workspace";
	}

	@Override
	public File getRoot() throws Exception {
		Location location = Platform.getInstanceLocation();

		return new File(location.getURL()
			.getPath());
	}

	@Override
	public boolean isEmpty() {
		return list.get()
			.isEmpty();
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		List<String> bsns = new ArrayList<>();

		if (pattern == null || pattern.equals("*") || pattern.equals("")) {
			bsns.addAll(list.get());
		} else {
			String[] split = pattern.split("\\s+");
			Glob globs[] = new Glob[split.length];
			for (int i = 0; i < split.length; i++) {
				globs[i] = new Glob(split[i]);
			}

			outer: for (String bsn : list.get()) {
				for (Glob g : globs) {
					if (g.matcher(bsn)
						.find()) {
						bsns.add(bsn);
						continue outer;
					}
				}
			}
		}

		logger.debug("{}: list({}) {}", getName(), pattern, bsns);
		return bsns;
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
		boolean changed = false;

		for (MavenProjectChangedEvent event : ((events != null) ? events : new MavenProjectChangedEvent[0])) {
			Kind kind = Kind.get(event);
			logger.debug("{}: mavenProjectChanged({}, {})", getName(), kind, event.getSource());
			switch (kind) {
				case ADDED :
				case CHANGED :
					if (process(event.getMavenProject(), monitor)) {
						changed = true;
					}
					break;
				case REMOVED :
					if (remove(event.getOldMavenProject()
						.getProject())) {
						changed = true;
					}
					break;
			}
		}

		if (changed) {
			list = memoize(this::list0);
			unchecked(() -> Central.refreshPlugins());
		}
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new IllegalStateException(getName() + " is read-only");
	}

	@Override
	public boolean refresh() throws Exception {
		if (process()) {
			list = memoize(this::list0);
			return true;
		}
		return false;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		Map<Requirement, Collection<Capability>> providers = findProviders(bsnRequirements.apply(bsn));

		SortedSet<Version> versions = providers.values()
			.stream()
			.flatMap(Collection<Capability>::stream)
			.map(Capability::getAttributes)
			.map(m -> m.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
			.map(String::valueOf)
			.map(Version::new)
			.collect(Collectors.toCollection(TreeSet<Version>::new));

		logger.debug("{}: versions({}) {}", getName(), bsn, versions);
		return versions;
	}

	@Override
	protected BiFunction<ResourceBuilder, File, ResourceBuilder> indexer(IProject project) {
		String name = project.getName();
		return (rb, file) -> {
			rb = fileIndexer(rb, file);
			if (rb == null) {
				return null;
			}

			boolean hasIdentity = rb.getCapabilities()
				.stream()
				.map(Capability::getNamespace)
				.anyMatch(IdentityNamespace.IDENTITY_NAMESPACE::equals);

			if (!hasIdentity) {
				try (Jar jar = new Jar(file)) {
					Optional<Revision> revision = jar.getPomXmlResources()
						.findFirst()
						.map(asFunctionOrElse(pomResource -> new POM(null, pomResource.openInputStream(), true), null))
						.map(POM::getRevision);

					if (!revision.isPresent()) {
						revision = Optional.of(new File(file.getAbsolutePath()
							.replaceFirst("\\.jar$", ".pom")))
							.filter(File::exists)
							.map(asFunction(FileInputStream::new))
							.map(asFunctionOrElse(is -> new POM(null, is, true),
								null))
							.map(POM::getRevision);
					}

					String identifier = jar.getModuleName();
					if (identifier == null) {
						identifier = revision.map(r -> r.program.toString())
							.orElse(null);
						if (identifier == null) {
							logger.debug("{}: Project {} ignore indexing file {}", getName(), project.getName(), file);

							return rb;
						}
					}

					Version version = revision.map(r -> r.version.getOSGiVersion())
						.orElse(null);
					if (version == null) {
						version = new MavenVersion(jar.getModuleVersion()).getOSGiVersion();
					}

					CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
						.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, identifier)
						.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version)
						.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_UNKNOWN);
					rb.addCapability(identity);
				} catch (Exception e) {
					logger.error("{}: Project {} error indexing file {}", getName(), project.getName(), file, e);
				}
			}

			if (logger.isDebugEnabled()) {
				String identifier = MapStream.of(//
					rb.getCapabilities()
						.stream()
						.map(Capability::getAttributes)
						.map(Map<String, Object>::entrySet)
						.flatMap(Set::stream)) //
					.filterKey(IdentityNamespace.IDENTITY_NAMESPACE::equals)
					.values()
					.map(String::valueOf)
					.findFirst()
					.orElse(null);

				logger.debug("{}: Project {} indexing file {} as {}", getName(), project.getName(), file, identifier);
			}

			return rb;
		};
	}

	@Override
	protected boolean isValid(IProject project) {
		try {
			return project.isOpen() && (mavenProjectRegistry.getProject(project) != null);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean process() {
		boolean changed = false;
		IProgressMonitor monitor = new NullProgressMonitor();
		for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
			if (process(projectFacade, monitor)) {
				changed = true;
			}
		}
		return changed;
	}

	private boolean process(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		Set<File> collected = collect(projectFacade, monitor);
		if (collected.isEmpty()) {
			return remove(projectFacade.getProject());
		} else {
			index(projectFacade.getProject(), collected);
			return true;
		}
	}

	private File get(final String bsn, final Version version) throws Exception {
		logger.trace("{}: get {} {}", getName(), bsn, version);

		Map<Requirement, Collection<Capability>> providers = findProviders(
			bsnAndVersionRequirements.apply(bsn, version));

		Resource resource = providers.values()
			.stream()
			.flatMap(Collection<Capability>::stream)
			.map(Capability::getResource)
			.findFirst()
			.orElse(null);

		if (resource == null) {
			logger.trace("{}: resource not found {} {}", getName(), bsn, version);
			return null;
		}
		ContentCapability content = ResourceUtils.getContentCapability(resource);
		if (content == null) {
			logger.warn("{}: No content capability for {}", getName(), resource);
			return null;
		}
		URI uri = content.url();
		if (uri == null) {
			logger.warn("{}: No content URI for {}", getName(), resource);
			return null;
		}
		logger.trace("{}: get returning {}", getName(), uri);
		return new File(uri);
	}

	private Set<String> list0() {
		Map<Requirement, Collection<Capability>> providers = findProviders(identityRequirements);

		return providers.values()
			.stream()
			.flatMap(Collection<Capability>::stream)
			.map(c -> c.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE))
			.map(
				String::valueOf)
			.collect(Collectors.toSet());
	}

	@Override
	protected boolean remove(IProject iProject) {
		logger.debug("{}: Removing project {}", getName(), iProject);
		return super.remove(iProject);
	}

	Set<File> collect(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		logger.debug("{}: Collecting files for project {}", getName(), projectFacade.getProject());

		Set<File> files = new HashSet<>();

		if (!isValid(projectFacade.getProject())) {
			logger.debug("{}: Project {} determined invalid", getName(), projectFacade.getProject());
			return files;
		}

		try {
			MavenProject mavenProject = projectFacade.getMavenProject(monitor);

			List<MojoExecution> mojoExecutions = projectFacade.getMojoExecutions( //
				"org.apache.maven.plugins", "maven-jar-plugin", monitor, "jar", "test-jar");

			logger.debug("Project {} found {} mojos", projectFacade.getFullPath(), mojoExecutions.size());

			for (MojoExecution mojoExecution : mojoExecutions) {
				String finalName = Optional.ofNullable( //
					maven.getMojoParameterValue(mavenProject, mojoExecution, "finalName", String.class, monitor))
					.orElse("");
				String classifier = Optional.ofNullable( //
					maven.getMojoParameterValue(mavenProject, mojoExecution, "classifier", String.class, monitor))
					.orElse("");

				StringBuilder fileName = new StringBuilder(finalName);
				if (!classifier.isEmpty()) {
					fileName.append("-")
						.append(classifier);
				}
				fileName.append(".jar");

				File bundleFile = new File(mavenProject.getBuild()
					.getDirectory(), fileName.toString());

				if (bundleFile.exists()) {
					logger.debug("{}: Collected file {} for project {}", getName(), bundleFile,
						projectFacade.getProject());
					files.add(bundleFile);
				}
			}
		} catch (Exception e) {
			logger.error("{}: Failed to collected files for project {}", getName(), projectFacade.getProject(), e);
		}

		return files;
	}

}
