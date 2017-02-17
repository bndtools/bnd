package aQute.bnd.maven.testing.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.RepositoryPlugin;
import aQute.libg.glob.Glob;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.StandaloneBndrun;

@Mojo(name = "testing", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestingMojo extends AbstractMojo {
	private static final Logger	logger			= LoggerFactory.getLogger(TestingMojo.class);

	@Parameter(property = "skipTests", defaultValue = "false")
	private boolean				skipTests;

	@Parameter(property = "maven.test.skip", defaultValue = "false")
	private boolean				skip;

	@Parameter(readonly = true, required = true)
	private List<File>	bndruns;

	@Parameter(defaultValue = "${project.build.directory}/test", readonly = true)
	private File				cwd;

	@Parameter(defaultValue = "${project.build.directory}/test-reports", readonly = true)
	private File				reportsDir;

	@Parameter(defaultValue = "${testing.select}", readonly = true)
	private File		testingSelect;

	@Parameter(defaultValue = "${testing}", readonly = true)
	private String		testing;

	@Parameter(defaultValue = "false")
	private boolean				resolve;

	@Parameter(defaultValue = "true")
	private boolean				failOnChanges;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	private int					errors	= 0;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || skipTests) {
			return;
		}
		try {
			if (testingSelect != null) {
				logger.info("Using selected testing file {}", testingSelect);
				testing(testingSelect);
			} else {

				Glob g = new Glob(testing == null ? "*" : testing);
				logger.info("Matching glob {}", g);

				for (File runFile : bndruns) {
					if (g.matcher(runFile.getName()).matches())
						testing(runFile);
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

	private void testing(File runFile) throws Exception {
		if (!runFile.exists()) {
			logger.error("Could not find bnd run file {}", runFile);
			errors++;
			return;
		}
		try (Bndrun run = new StandaloneBndrun(runFile)) {
			Workspace workspace = run.getWorkspace();
			workspace.setOffline(session.getSettings().isOffline());
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			report(run);
			if (!run.isOk()) {
				return;
			}
			if (resolve) {
				String runBundles = run.resolve(failOnChanges, false);
				report(run);
				if (!run.isOk()) {
					return;
				}
				run.setProperty(Constants.RUNBUNDLES, runBundles);
			}
			String bndrun = getNamePart(runFile);
			File bndrunCwd = new File(cwd, bndrun);
			File bndrunResportsDir = new File(reportsDir, bndrun);
			run.test(bndrunCwd, bndrunResportsDir);
			report(run);
		}
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf(".");
		return nameExt.substring(0, pos);
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
