package bndtools.m2e;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.build.Run;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.BndrunContainer.Builder;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import bndtools.central.Central;

public class MavenImplicitProjectRepository extends AbstractMavenRepository implements Refreshable {

	private static final ILogger						logger					= Logger
		.getLogger(MavenImplicitProjectRepository.class);

	private static final Version						scopesVersion			= new Version(4, 2, 0);

	private static final IProjectConfigurationManager	configurationManager	= MavenPlugin
		.getProjectConfigurationManager();
	private static final ProjectRegistryManager			projectManager			= MavenPluginActivator.getDefault()
		.getMavenProjectManagerImpl();

	private volatile FileSetRepository					fileSetRepository;

	private final IMavenProjectFacade					projectFacade;
	private final File									bndrunFile;
	private final Run									run;

	public MavenImplicitProjectRepository(IMavenProjectFacade projectFacade, File bndrunFile, Run run) {
		this.projectFacade = projectFacade;
		this.bndrunFile = bndrunFile;
		this.run = run;

		mavenProjectRegistry.addMavenProjectChangedListener(this);
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		if (fileSetRepository == null) {
			return Collections.emptyMap();
		}
		return fileSetRepository.findProviders(requirements);
	}

	@Override
	public File get(final String bsn, final Version version, Map<String, String> properties,
		final DownloadListener... listeners) throws Exception {
		if (fileSetRepository == null) {
			return null;
		}
		return fileSetRepository.get(bsn, version, properties, listeners);
	}

	@Override
	public String getName() {
		return projectFacade.getProject()
			.getName() + " (implicit)";
	}

	@Override
	public File getRoot() throws Exception {
		return projectFacade.getPomFile();
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		if (fileSetRepository == null) {
			return Collections.emptyList();
		}
		return fileSetRepository.list(pattern);
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
		if (events == null)
			return;

		for (MavenProjectChangedEvent event : events) {
			final IMavenProjectFacade mavenProjectFacade = event.getMavenProject();

			if (!mavenProjectFacade.getProject()
				.equals(projectFacade.getProject())
				&& (event.getFlags() != MavenProjectChangedEvent.FLAG_DEPENDENCIES)) {

				continue;
			}

			createRepo(mavenProjectFacade, monitor);
		}
	}

	@Override
	public boolean refresh() throws Exception {
		return true;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		if (fileSetRepository == null) {
			return new TreeSet<Version>();
		}
		return fileSetRepository.versions(bsn);
	}

	protected void createRepo(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		MavenProject mavenProject = getMavenProject(projectFacade);
		try {
			ResolverConfiguration resolverConfiguration = configurationManager
				.getResolverConfiguration(projectFacade.getProject());
			IMavenExecutionContext context = projectManager.createExecutionContext(projectFacade.getPom(),
				resolverConfiguration);

			Entry<MavenSession, RepositorySystemSession> sessions = context.execute(mavenProject,
				(context1, monitor1) -> new SimpleEntry<>(context1.getSession(), context1.getRepositorySession()),
				monitor);

			MavenSession mavenSession = sessions.getKey();
			mavenSession.setCurrentProject(mavenProject);

			@SuppressWarnings("deprecation")
			Builder containerBuilder = new BndrunContainer.Builder(mavenProject, mavenSession, sessions.getValue(),
				lookupComponent(ProjectDependenciesResolver.class),
				lookupComponent(org.apache.maven.artifact.factory.ArtifactFactory.class),
				lookupComponent(RepositorySystem.class));

			MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject,
				Collections.singletonList("bnd-resolver:resolve"),
				true, monitor);
			MojoExecution mojoExecution = plan.getMojoExecutions()
				.stream()
				.filter(exe -> containsBndrun(exe, mavenProject, monitor))
				.findFirst()
				.orElse(null);

			if (mojoExecution != null) {
				Bundles bundles = maven.getMojoParameterValue(mavenProject, mojoExecution, "bundles", Bundles.class,
					monitor);
				if (bundles == null) {
					bundles = new Bundles();
				}

				containerBuilder.setBundles(bundles.getFiles(mavenProject.getBasedir()));

				containerBuilder.setUseMavenDependencies(maven.getMojoParameterValue(mavenProject, mojoExecution,
					"useMavenDependencies", Boolean.class, monitor));

				// Comparing OSGi versions makes '4.2.0.SNAPSHOT' > '4.2.0'.
				// Comparing Maven versions fails because '4.2.0-SNAPSHOT' is <
				// '4.2.0'.
				Version mojoVersion = new MavenVersion(mojoExecution.getVersion()).getOSGiVersion();

				if (mojoVersion.compareTo(scopesVersion) >= 0) {
					containerBuilder.setIncludeDependencyManagement(maven.getMojoParameterValue(mavenProject,
						mojoExecution, "includeDependencyManagement", Boolean.class, monitor));

					@SuppressWarnings("unchecked")
					Set<String> scopeValues = maven.getMojoParameterValue(mavenProject, mojoExecution, "scopes",
						Set.class, monitor);
					if (!scopeValues.isEmpty()) {
						containerBuilder.setScopes(scopeValues.stream()
							.map(Scope::valueOf)
							.collect(Collectors.toSet()));
					}
				}
			}

			containerBuilder.setPostProcessor(new WorkspaceProjectPostProcessor(monitor));

			BndrunContainer bndrunContainer = containerBuilder.build();
			bndrunContainer.setRunrequiresFromProjectArtifact(run);
			bndrunContainer.setEEfromBuild(run);

			fileSetRepository = bndrunContainer.getFileSetRepository();
			fileSetRepository.list(null);

			Central.refreshPlugin(this);
		} catch (Exception e) {
			logger.logError("Failed to create implicit repository for m2e project " + getName(), e);

			try {
				String name = mavenProject.getName()
					.isEmpty() ? mavenProject.getArtifactId() : mavenProject.getName();

				fileSetRepository = new FileSetRepository(name, Collections.emptyList());

				Central.refreshPlugin(this);
			} catch (Exception e2) {
				throw Exceptions.duck(e2);
			}
		}
	}

	private boolean containsBndrun(MojoExecution mojoExecution, MavenProject mavenProject, IProgressMonitor monitor) {
		try {
			Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
				monitor);

			return bndruns.getFiles(mavenProject.getBasedir())
				.contains(bndrunFile);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
