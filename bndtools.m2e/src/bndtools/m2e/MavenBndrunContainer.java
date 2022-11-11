package bndtools.m2e;

import static aQute.bnd.exceptions.Exceptions.unchecked;
import static aQute.bnd.exceptions.FunctionWithException.asFunction;
import static bndtools.m2e.MavenRunListenerHelper.getMavenProject;
import static bndtools.m2e.MavenRunListenerHelper.lookupComponent;

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
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.BndrunContainer.Builder;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import biz.aQute.resolve.Bndrun;

public class MavenBndrunContainer {

	private static final ILogger						logger					= Logger
		.getLogger(MavenBndrunContainer.class);

	private static final Version						scopesVersion			= new Version(4, 2, 0);

	private final IMavenProjectRegistry	mavenProjectRegistry;
	private final BndrunContainer						bndrunContainer;
	private final IMavenProjectFacade					mavenProjectFacade;
	private final IProgressMonitor						monitor;

	public static MavenBndrunContainer getBndrunContainer(IMaven maven, IMavenProjectRegistry mavenProjectRegistry,
		IMavenProjectFacade projectFacade, MojoExecution mojoExecution, IProgressMonitor monitor) {

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

		return getBndrunContainer(maven, mavenProjectRegistry, projectFacade, bundles, useMavenDependencies,
			includeDependencyManagement, scopeValues, monitor);
	}

	public static MavenBndrunContainer getBndrunContainer(IMaven maven, IMavenProjectRegistry mavenProjectRegistry,
		IMavenProjectFacade projectFacade, Bundles bundles,
		boolean useMavenDependencies, boolean includeDependencyManagement, Set<String> scopeValues,
		IProgressMonitor monitor) {
		MavenProject mavenProject = getMavenProject(projectFacade);
		try {
			BndrunContainer bndrunContainer = mavenProjectRegistry.execute(projectFacade, (c, m) -> {
				MavenSession mavenSession = c.getSession();
				mavenSession.setCurrentProject(mavenProject);

				@SuppressWarnings("deprecation")
				Builder builder = new BndrunContainer.Builder(mavenProject, mavenSession, c.getRepositorySession(),
					lookupComponent(maven, ProjectDependenciesResolver.class),
					lookupComponent(maven, org.apache.maven.artifact.factory.ArtifactFactory.class),
					lookupComponent(maven, RepositorySystem.class));

				Optional.ofNullable(bundles)
					.map(asFunction(b -> b.getFiles(mavenProject.getBasedir())))
					.ifPresent(builder::setBundles);

				builder.setIncludeDependencyManagement(includeDependencyManagement);

				if ((scopeValues != null) && !scopeValues.isEmpty()) {
					builder.setScopes(scopeValues.stream()
						.map(Scope::valueOf)
						.collect(Collectors.toSet()));
				}

				builder.setPostProcessor(new WorkspaceProjectPostProcessor(mavenProjectRegistry, m));

				return builder.build();
			}, monitor);

			return new MavenBndrunContainer(bndrunContainer, mavenProjectRegistry, projectFacade, monitor);
		} catch (Exception e) {
			logger.logError("Failed to create Run for m2e project " + mavenProject.getName(), e);

			return null;
		}
	}

	public MavenBndrunContainer(BndrunContainer bndrunContainer, IMavenProjectRegistry mavenProjectRegistry,
		IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		this.bndrunContainer = bndrunContainer;
		this.mavenProjectRegistry = mavenProjectRegistry;
		this.mavenProjectFacade = projectFacade;
		this.monitor = monitor;
	}

	public Bndrun init(File runFile, String task, File workingDir) throws Exception {
		return mavenProjectRegistry.execute(mavenProjectFacade,
			(c, m) -> unchecked(() -> bndrunContainer.init(runFile, task, workingDir)), monitor);
	}

	public Map<File, ArtifactResult> resolve() throws Exception {
		return mavenProjectRegistry.execute(mavenProjectFacade,
			(c, m) -> unchecked(() -> bndrunContainer.getDependencyResolver(mavenProjectFacade.getMavenProject())
				.resolve()),
			monitor);
	}

}
