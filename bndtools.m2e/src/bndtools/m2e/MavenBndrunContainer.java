package bndtools.m2e;

import static aQute.bnd.exceptions.Exceptions.unchecked;
import static aQute.bnd.exceptions.FunctionWithException.asFunction;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;

import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.BndrunContainer.Builder;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import biz.aQute.resolve.Bndrun;

public class MavenBndrunContainer implements MavenRunListenerHelper {

	private static final ILogger						logger					= Logger
		.getLogger(MavenBndrunContainer.class);

	private static final Version						scopesVersion			= new Version(4, 2, 0);

	private static final IProjectConfigurationManager	configurationManager	= MavenPlugin
		.getProjectConfigurationManager();
	private static final ProjectRegistryManager			projectManager			= MavenPluginActivator.getDefault()
		.getMavenProjectManagerImpl();

	private static final MavenRunListenerHelper			helper					= new MavenRunListenerHelper() {};

	private final BndrunContainer						bndrunContainer;
	private final MavenProject							mavenProject;
	private final IMavenExecutionContext				context;
	private final IProgressMonitor						monitor;

	public static MavenBndrunContainer getBndrunContainer(IMavenProjectFacade projectFacade,
		MojoExecution mojoExecution, IProgressMonitor monitor) {

		MavenProject mavenProject = helper.getMavenProject(projectFacade);

		Bundles bundles = null;
		boolean useMavenDependencies = true;
		boolean includeDependencyManagement = false;
		Set<String> scopeValues = null;

		if (mojoExecution != null) {
			try {
				bundles = maven.getMojoParameterValue(mavenProject, mojoExecution, "bundles", Bundles.class, monitor);
				if (bundles == null) {
					bundles = new Bundles();
				}

				useMavenDependencies = maven.getMojoParameterValue(mavenProject, mojoExecution, "useMavenDependencies",
					Boolean.class, monitor);

				// Comparing OSGi versions makes '4.2.0.SNAPSHOT' > '4.2.0'.
				// Comparing Maven versions fails because '4.2.0-SNAPSHOT' is <
				// '4.2.0'.
				Version mojoVersion = new MavenVersion(mojoExecution.getVersion()).getOSGiVersion();

				if (mojoVersion.compareTo(scopesVersion) >= 0) {
					includeDependencyManagement = maven.getMojoParameterValue(mavenProject, mojoExecution,
						"includeDependencyManagement", Boolean.class, monitor);

					@SuppressWarnings({
						"rawtypes", "unchecked"
					})
					Class<Set<String>> asType = (Class) Set.class;
					scopeValues = maven.getMojoParameterValue(mavenProject, mojoExecution, "scopes", asType, monitor);
				}
			} catch (Exception e) {
				logger.logError("Failed to create Run for m2e project " + mavenProject.getName(), e);

				return null;
			}
		}

		return getBndrunContainer(projectFacade, bundles, useMavenDependencies, includeDependencyManagement,
			scopeValues, monitor);
	}

	public static MavenBndrunContainer getBndrunContainer(IMavenProjectFacade projectFacade, Bundles bundles,
		boolean useMavenDependencies, boolean includeDependencyManagement, Set<String> scopeValues,
		IProgressMonitor monitor) {
		MavenProject mavenProject = helper.getMavenProject(projectFacade);
		try {
			ResolverConfiguration resolverConfiguration = configurationManager
				.getResolverConfiguration(projectFacade.getProject());
			IMavenExecutionContext context = projectManager.createExecutionContext(projectFacade.getPom(),
				resolverConfiguration);

			BndrunContainer bndrunContainer = context.execute(mavenProject, (c, m) -> {
				MavenSession mavenSession = c.getSession();
				mavenSession.setCurrentProject(mavenProject);

				@SuppressWarnings("deprecation")
				Builder builder = new BndrunContainer.Builder(mavenProject, mavenSession, c.getRepositorySession(),
					helper.lookupComponent(ProjectDependenciesResolver.class),
					helper.lookupComponent(org.apache.maven.artifact.factory.ArtifactFactory.class),
					helper.lookupComponent(RepositorySystem.class));

				Optional.ofNullable(bundles)
					.map(asFunction(b -> b.getFiles(mavenProject.getBasedir())))
					.ifPresent(builder::setBundles);

				builder.setIncludeDependencyManagement(includeDependencyManagement);

				if ((scopeValues != null) && !scopeValues.isEmpty()) {
					builder.setScopes(scopeValues.stream()
						.map(Scope::valueOf)
						.collect(Collectors.toSet()));
				}

				builder.setPostProcessor(new WorkspaceProjectPostProcessor(m));

				return builder.build();
			}, monitor);

			return new MavenBndrunContainer(bndrunContainer, mavenProject, context, monitor);
		} catch (Exception e) {
			logger.logError("Failed to create Run for m2e project " + mavenProject.getName(), e);

			return null;
		}
	}

	public MavenBndrunContainer(BndrunContainer bndrunContainer, MavenProject mavenProject,
		IMavenExecutionContext context, IProgressMonitor monitor) {
		this.bndrunContainer = bndrunContainer;
		this.mavenProject = mavenProject;
		this.context = context;
		this.monitor = monitor;
	}

	public Bndrun init(File runFile, String task, File workingDir) throws Exception {
		return context.execute( //
			(c, m) -> unchecked(() -> bndrunContainer.init(runFile, task, workingDir)), monitor);
	}

	public Map<File, ArtifactResult> resolve() throws Exception {
		return context.execute( //
			(c, m) -> unchecked(() -> bndrunContainer.getDependencyResolver(mavenProject)
				.resolve()),
			monitor);
	}

}
