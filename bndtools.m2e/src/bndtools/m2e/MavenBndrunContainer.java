package bndtools.m2e;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
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
import org.eclipse.m2e.core.project.ResolverConfiguration;

import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.BndrunContainer.Builder;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;

public class MavenBndrunContainer implements MavenRunListenerHelper {

	private static final ILogger						logger					= Logger
		.getLogger(MavenBndrunContainer.class);

	private static final Version						scopesVersion			= new Version(4, 2, 0);

	private static final IProjectConfigurationManager	configurationManager	= MavenPlugin
		.getProjectConfigurationManager();
	private static final ProjectRegistryManager			projectManager			= MavenPluginActivator.getDefault()
		.getMavenProjectManagerImpl();

	public BndrunContainer getBndrunContainer(IMavenProjectFacade projectFacade, MojoExecution mojoExecution,
		IProgressMonitor monitor) {

		MavenProject mavenProject = getMavenProject(projectFacade);

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

		return getBndrunContainer(projectFacade, bundles, useMavenDependencies, includeDependencyManagement, scopeValues, monitor);
	}

	public BndrunContainer getBndrunContainer(IMavenProjectFacade projectFacade, Bundles bundles,
		boolean useMavenDependencies, boolean includeDependencyManagement, Set<String> scopeValues,
		IProgressMonitor monitor) {
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

			if (bundles == null) {
				bundles = new Bundles();
			}

			containerBuilder.setBundles(bundles.getFiles(mavenProject.getBasedir()));

			containerBuilder.setIncludeDependencyManagement(includeDependencyManagement);

			if ((scopeValues != null) && !scopeValues.isEmpty()) {
				containerBuilder.setScopes(scopeValues.stream()
					.map(Scope::valueOf)
					.collect(Collectors.toSet()));
			}

			containerBuilder.setPostProcessor(new WorkspaceProjectPostProcessor(monitor));

			BndrunContainer bndrunContainer = containerBuilder.build();

			return bndrunContainer;
		} catch (Exception e) {
			logger.logError("Failed to create Run for m2e project " + mavenProject.getName(), e);

			return null;
		}
	}

}
