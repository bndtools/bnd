package bndtools.m2e;

import static aQute.bnd.exceptions.Exceptions.unchecked;

import java.io.File;
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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.MavenCapability;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.AbstractIndexingRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.BridgeRepository.InfoCapability;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.bnd.exceptions.Exceptions;
import aQute.libg.glob.Glob;
import bndtools.central.Central;

@Component(service = {
	IMavenProjectChangedListener.class, MavenWorkspaceRepository.class, RepositoryPlugin.class
})
public class MavenWorkspaceRepository extends AbstractIndexingRepository<IProject, Artifact>
	implements IMavenProjectChangedListener, MavenRunListenerHelper, PopulatedRepository, RepositoryPlugin, Actionable {

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

	private final static Logger											logger			= LoggerFactory
		.getLogger(MavenWorkspaceRepository.class);
	private final static PromiseFactory									promiseFactory	= Processor.getPromiseFactory();

	private final static String											BND_INFO		= "bnd.info";
	private volatile Supplier<Set<String>>								list;
	private final Function<String, RequirementExpression>				identificationExpressionFunction;
	private final BiFunction<String, Version, RequirementExpression>	identificationAndVersionExpressionFunction;
	private final RequirementExpression									identificationExpression;

	public MavenWorkspaceRepository() {
		super();

		ExpressionCombiner combiner = getExpressionCombiner();

		identificationExpression = combiner.or( //
			combiner.identity(ResourceUtils.createWildcardRequirement()), //
			combiner.identity(new RequirementBuilder(BND_INFO).addFilter("(name=*)")
				.buildSyntheticRequirement()));

		identificationExpressionFunction = bsn -> {
			RequirementBuilder builder = new RequirementBuilder(BND_INFO);
			builder.addFilter("name", bsn, null, null);
			return combiner.or( //
				combiner.identity(CapReqBuilder.createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn, null)
					.buildSyntheticRequirement()), //
				combiner.identity(builder.buildSyntheticRequirement()));
		};

		identificationAndVersionExpressionFunction = (bsn, version) -> {
			RequirementBuilder builder = new RequirementBuilder(BND_INFO);
			builder.addFilter("name", bsn, version.toString(), null);
			return combiner.or( //
				combiner.identity(
					CapReqBuilder.createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn, version.toString())
						.buildSyntheticRequirement()), //
				combiner.identity(builder.buildSyntheticRequirement()));
		};
	}

	@Activate
	void activate() {
		list = memoize(this::list0);
		promiseFactory.submit(this::process)
			.thenAccept(changed -> {
				if (changed) {
					list = memoize(this::list0);
					Central.refreshPlugins();
				}
			});
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
			logger.debug("{}: mavenProjectChanged resulting in a refresh", getName());
			list = memoize(this::list0);
			unchecked(Central::refreshPlugins);
		}
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new IllegalStateException(getName() + " is read-only");
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		Promise<Collection<Resource>> promise = findProviders(identificationExpressionFunction.apply(bsn));

		Throwable failure = promise.getFailure();

		if (failure != null) {
			throw Exceptions.duck(failure);
		}

		SortedSet<Version> versions = promise.getValue()
			.stream()
			.map(r -> {
				InfoCapability info = getInfo(r);
				if (info != null) {
					return info.version();
				}
				IdentityCapability identity = ResourceUtils.getIdentityCapability(r);
				return identity.version();
			})
			.collect(Collectors.toCollection(TreeSet<Version>::new));

		logger.debug("{}: versions({}) {}", getName(), bsn, versions);
		return versions;
	}

	@Override
	protected BiFunction<ResourceBuilder, Artifact, ResourceBuilder> indexer(IProject project) {
		String name = project.getName();
		return (rb, artifact) -> {
			try {
				rb = fileIndexer(rb, artifact.getFile());
				if (rb == null) {
					return null;
				}

				Optional<IdentityCapability> identityCapability = rb.getCapabilities()
					.stream()
					.filter(c -> c.getNamespace()
						.equals(IdentityNamespace.IDENTITY_NAMESPACE))
					.map(c -> ResourceUtils.as(c, IdentityCapability.class))
					.findFirst();

				String identifier = identityCapability.map(IdentityCapability::osgi_identity)
					.orElseGet(() -> artifact.getGroupId()
						.concat(":")
						.concat(artifact.getArtifactId()));

				MavenVersion mavenVersion = new MavenVersion(artifact.getVersion());
				String from = artifact.getProperty("from", "");

				if (!identityCapability.isPresent()) {
					BridgeRepository.addInformationCapability(rb, identifier, mavenVersion.getOSGiVersion(), from,
						Constants.NOT_A_BUNDLE_S);
				}

				MavenCapability.addMavenCapability(rb, artifact.getGroupId(), artifact.getArtifactId(), mavenVersion,
					artifact.getClassifier(), from);

				if (logger.isDebugEnabled()) {
					logger.debug("{}: Project {} indexing artifact {} as {}", getName(), project.getName(), artifact,
						identifier);
				}
			} catch (Throwable t) {
				logger.error("{}: Error in {} indexing artifact {}", getName(), project.getName(), artifact, t);
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
		Set<Artifact> collected = collect(projectFacade, monitor);
		if (collected.isEmpty()) {
			return remove(projectFacade.getProject());
		} else {
			index(projectFacade.getProject(), collected);
			return true;
		}
	}

	private Resource getResource(final String bsn) throws Exception {
		Promise<Collection<Resource>> promise = findProviders(identificationExpressionFunction.apply(bsn));

		Throwable failure = promise.getFailure();

		if (failure != null) {
			throw Exceptions.duck(failure);
		}

		return promise.getValue()
			.stream()
			.findFirst()
			.orElse(null);
	}

	private Resource getResource(final String bsn, final Version version) throws Exception {
		Promise<Collection<Resource>> promise = findProviders(
			identificationAndVersionExpressionFunction.apply(bsn, version));

		Throwable failure = promise.getFailure();

		if (failure != null) {
			throw Exceptions.duck(failure);
		}

		return promise.getValue()
			.stream()
			.findFirst()
			.orElse(null);
	}

	private File get(final String bsn, final Version version) throws Exception {
		logger.trace("{}: get {} {}", getName(), bsn, version);

		Resource resource = getResource(bsn, version);

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
		Promise<Collection<Resource>> promise = findProviders(identificationExpression);

		try {
			Throwable failure = promise.getFailure();

			if (failure != null) {
				throw Exceptions.duck(failure);
			}

			return promise.getValue()
				.stream()
				.map(r -> {
					InfoCapability info = getInfo(r);
					if (info != null) {
						return info.name();
					}
					IdentityCapability identity = ResourceUtils.getIdentityCapability(r);
					return identity.osgi_identity();
				})
				.collect(Collectors.toSet());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	protected boolean remove(IProject iProject) {
		logger.debug("{}: Removing project {}", getName(), iProject);
		return super.remove(iProject);
	}

	Set<Artifact> collect(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		logger.debug("{}: Collecting files for project {}", getName(), projectFacade.getProject());

		Set<Artifact> files = new HashSet<>();

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
					Artifact artifact = new DefaultArtifact(mavenProject.getGroupId(), mavenProject.getArtifactId(),
						classifier, "jar", mavenProject.getVersion(),
						Collections.singletonMap("from", projectFacade.getProject()
							.toString()),
						bundleFile);
					files.add(artifact);
				}
			}
		} catch (Exception e) {
			logger.error("{}: Failed to collected files for project {}", getName(), projectFacade.getProject(), e);
		}

		return files;
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		switch (target.length) {
			case 0 :
				return getName();
			case 1 :
				return getArtifactName((String) target[0]);
			case 2 :
				return getVersionName((String) target[0], (Version) target[1]);
			default :
				return null;
		}
	}

	private String getVersionName(String bsn, Version version) throws Exception {
		Resource resource = getResource(bsn, version);
		IdentityCapability identityCapability = ResourceUtils.getIdentityCapability(resource);
		if (identityCapability != null) {
			return identityCapability.version()
				.toString();
		}
		InfoCapability info = getInfo(resource);
		if (info.error()
			.equals(Constants.NOT_A_BUNDLE_S)) {
			return String.format("%s [%s]", info.version(), info.error());
		}
		return info.version()
			.toString();
	}

	private String getArtifactName(String bsn) throws Exception {
		Resource resource = getResource(bsn);
		IdentityCapability identityCapability = ResourceUtils.getIdentityCapability(resource);
		if (identityCapability != null) {
			return identityCapability.osgi_identity();
		}
		InfoCapability info = getInfo(resource);
		if (info.error()
			.equals(Constants.NOT_A_BUNDLE_S)) {
			return String.format("%s [!]", info.name());
		}
		return bsn;
	}

	static InfoCapability getInfo(Resource resource) {
		return ResourceUtils.capabilityStream(resource, BND_INFO, InfoCapability.class)
			.findFirst()
			.orElse(null);
	}

}
