package aQute.bnd.maven.run.plugin;

import static aQute.bnd.maven.lib.resolve.BndrunContainer.report;

import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.Scope;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

@Mojo(name = "run", defaultPhase = LifecyclePhase.VERIFY, requiresDirectInvocation = true, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(goal = "run")
public class RunMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject										project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession								repositorySession;

	@Parameter(required = true)
	private File 												bndrun;

	@Parameter(required = false)
	private Bundles												bundles	= new Bundles();

	@Parameter(defaultValue = "true")
	private boolean												useMavenDependencies;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession										session;

	@Parameter(property = "bnd.run.scopes", defaultValue = "compile,runtime")
	private Set<Scope>											scopes	= new HashSet<>(
		Arrays.asList(Scope.compile, Scope.runtime));

	@Parameter(property = "bnd.run.include.dependency.management", defaultValue = "false")
	private boolean												includeDependencyManagement;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File												targetDir;

	@Component
	private RepositorySystem									system;

	@Component
	private ProjectDependenciesResolver							resolver;

	@Component
	@SuppressWarnings("deprecation")
	private org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		int errors = 0;

		try {
			BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
				artifactFactory, system).setBundles(bundles.getFiles(project.getBasedir()))
					.setIncludeDependencyManagement(includeDependencyManagement)
					.setScopes(scopes)
					.setUseMavenDependencies(useMavenDependencies)
					.build();

			Operation operation = getOperation();

			Bndruns bndruns = new Bndruns();
			bndruns.addFile(bndrun);
			List<File> files = bndruns.getFiles(project.getBasedir(), "*.bndrun");

			errors += container.execute(files.get(0), "run", targetDir, operation);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private Operation getOperation() {
		return (file, runName, run) -> {
			try {
				run.run();
			} finally {
				int errors = report(run);
				if (errors > 0) {
					return errors;
				}
			}
			return 0;
		};
	}

}
