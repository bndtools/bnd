package aQute.bnd.maven.resolver.plugin;

import static aQute.bnd.maven.lib.resolve.BndrunContainer.report;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.osgi.Constants;
import biz.aQute.resolve.ResolveProcess;

/**
 * Resolves the <code>-runbundles</code> for the given bndrun file.
 */
@Mojo(name = "resolve", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ResolverMojo extends AbstractMojo {
	private static final Logger									logger	= LoggerFactory.getLogger(ResolverMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject										project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings											settings;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession								repositorySession;

	@Parameter
	private Bndruns												bndruns	= new Bndruns();

	@Parameter
	private Bundles												bundles	= new Bundles();

	@Parameter(defaultValue = "true")
	private boolean												useMavenDependencies;

	@Parameter(defaultValue = "true")
	private boolean												failOnChanges;

	@Parameter(defaultValue = "true")
	private boolean												writeOnChanges;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File												targetDir;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession										session;

	@Parameter(property = "bnd.resolve.include.dependency.management", defaultValue = "false")
	private boolean												includeDependencyManagement;

	@Parameter(defaultValue = "true")
	private boolean												reportOptional;

	@Parameter(property = "bnd.resolve.scopes", defaultValue = "compile,runtime")
	private Set<Scope>											scopes	= new HashSet<>(
		Arrays.asList(Scope.compile, Scope.runtime));

	@Parameter(property = "bnd.resolve.skip", defaultValue = "false")
	private boolean												skip;

	@Component
	private RepositorySystem									system;

	@Component
	private ProjectDependenciesResolver							resolver;

	@Component
	@SuppressWarnings("deprecation")
	private org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		int errors = 0;

		try {
			BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
				artifactFactory, system).setBundles(bundles.getFiles(project.getBasedir()))
					.setIncludeDependencyManagement(includeDependencyManagement)
					.setScopes(scopes)
					.setUseMavenDependencies(useMavenDependencies)
					.build();

			Operation operation = getOperation();

			for (File runFile : bndruns.getFiles(project.getBasedir(), "*.bndrun")) {
				errors += container.execute(runFile, "resolve", targetDir, operation);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private Operation getOperation() {
		return (file, runName, run) -> {
			try {
				String result = run.resolve(failOnChanges, writeOnChanges);
				logger.info("{}: {}", Constants.RUNBUNDLES, result);
			} catch (ResolutionException re) {
				logger.error(ResolveProcess.format(re, reportOptional));
				throw re;
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
