package aQute.bnd.maven.testing.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.osgi.Constants;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ResolveProcess;

@Mojo(name = "testing", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestingMojo extends AbstractMojo {
	private static final Logger			logger	= LoggerFactory.getLogger(TestingMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject				project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession		repositorySession;

	@Parameter(property = "skipTests", defaultValue = "false")
	private boolean						skipTests;

	@Parameter(property = "maven.test.skip", defaultValue = "false")
	private boolean						skip;

	@Parameter(readonly = true, required = true)
	private List<File>					bndruns;

	@Parameter(defaultValue = "${project.build.directory}/test", readonly = true)
	private File						cwd;

	@Parameter(defaultValue = "${project.build.directory}/test-reports", readonly = true)
	private File						reportsDir;

	@Parameter(defaultValue = "${testing.select}", readonly = true)
	private File						testingSelect;

	@Parameter(defaultValue = "${testing}", readonly = true)
	private String						testing;

	@Parameter(readonly = true, required = false)
	private List<File>					bundles;

	@Parameter(defaultValue = "true")
	private boolean						useMavenDependencies;

	@Parameter(defaultValue = "false")
	private boolean						resolve;

	@Parameter(defaultValue = "true")
	private boolean						failOnChanges;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession				session;

	private int							errors	= 0;

	@Component
	private RepositorySystem			system;

	@Component
	private ProjectDependenciesResolver	resolver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || skipTests) {
			return;
		}
		try {
			DependencyResolver dependencyResolver = new DependencyResolver(project, repositorySession, resolver,
				system);

			FileSetRepository fileSetRepository = dependencyResolver.getFileSetRepository(project.getName(), bundles,
				useMavenDependencies);

			if (testingSelect != null) {
				logger.info("Using selected testing file {}", testingSelect);
				testing(testingSelect, fileSetRepository);
			} else {

				Glob g = new Glob(testing == null ? "*" : testing);
				logger.info("Matching glob {}", g);

				for (File runFile : bndruns) {
					if (g.matcher(runFile.getName())
						.matches())
						testing(runFile, fileSetRepository);
					else
						logger.info("Skipping {}", g);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoExecutionException(errors + " errors found");
	}

	private void testing(File runFile, FileSetRepository fileSetRepository) throws Exception {
		if (!runFile.exists()) {
			logger.error("Could not find bnd run file {}", runFile);
			errors++;
			return;
		}
		String bndrun = getNamePart(runFile);
		File workingDir = new File(cwd, bndrun);
		File cnf = new File(workingDir, Workspace.CNFDIR);
		IO.mkdirs(cnf);
		try (Bndrun run = Bndrun.createBndrun(null, runFile)) {
			run.setBase(workingDir);
			Workspace workspace = run.getWorkspace();
			workspace.setBuildDir(cnf);
			workspace.setOffline(session.getSettings()
				.isOffline());
			workspace.addBasicPlugin(fileSetRepository);
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			run.getInfo(workspace);
			report(run);
			if (!run.isOk()) {
				return;
			}
			if (resolve) {
				try {
					String runBundles = run.resolve(failOnChanges, false);
					if (!run.isOk()) {
						return;
					}
					run.setProperty(Constants.RUNBUNDLES, runBundles);
				} catch (ResolutionException re) {
					logger.error("Unresolved requirements: {}", ResolveProcess.format(re.getUnresolvedRequirements()));
					throw re;
				} finally {
					report(run);
				}
			}
			try {
				run.test(new File(reportsDir, bndrun), null);
			} finally {
				report(run);
			}
		}
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf('.');
		return (pos > 0) ? nameExt.substring(0, pos) : nameExt;
	}

	private void report(Bndrun run) {
		for (String warning : run.getWarnings()) {
			logger.warn("Warning : {}", warning);
		}
		for (String error : run.getErrors()) {
			logger.error("Error   : {}", error);
			errors++;
		}
	}

}
