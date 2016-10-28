package aQute.bnd.maven.testing.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
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

import aQute.bnd.build.Container;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import biz.aQute.resolve.ProjectResolver;

@Mojo(name = "testing", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestingMojo extends AbstractMojo {
	private static final Logger	logger			= LoggerFactory.getLogger(TestingMojo.class);

	@Parameter(readonly = true, required = true)
	private List<File>	bndruns;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File		targetDir;

	@Parameter(defaultValue = "${testing.select}", readonly = true)
	private File		testingSelect;

	@Parameter(defaultValue = "${testing}", readonly = true)
	private String		testing;

	@Parameter(readonly = true, required = false)
	private boolean		resolve			= false;

	@Parameter(readonly = true, required = false)
	private boolean		failOnChanges	= true;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	public void execute() throws MojoExecutionException, MojoFailureException {
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
	}

	private void testing(File runFile) throws MojoExecutionException, Exception, IOException {
		if (!runFile.exists()) {
			throw new MojoExecutionException("Could not find bnd run file " + runFile);
		}
		try (StandaloneRun run = new StandaloneRun(runFile)) {
			Workspace workspace = run.getWorkspace();
			workspace.setOffline(session.getSettings().isOffline());
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			run.check();
			if (!run.isOk()) {
				throw new MojoExecutionException("Initializing the workspace failed " + run.getErrors());
			}

			if (resolve) {
				resolve(run);
			}
			run.setProperty("-runbundles.tester", "biz.aQute.tester");
			testing(run);
		}
	}

	private void testing(Run run) throws Exception {
		try {
			System.out.println("Test " + run);
			ProjectTester projectTester = run.getProjectTester();
			File dir = new File(targetDir, "tmp");
			IO.delete(dir);
			dir.mkdirs();

			projectTester.setCwd(dir);
			projectTester.setReportDir(targetDir);
			projectTester.test();

			if (!run.getErrors().isEmpty()) {
				System.out.println("Errors");
				System.out.println(Strings.join("\n", run.getErrors()));
				System.out.println();
			}
			if (!run.getWarnings().isEmpty()) {
				System.out.println("Warnings");
				System.out.println(Strings.join("\n", run.getWarnings()));
				System.out.println();
			}

			IO.delete(dir);

			if (run.getErrors().isEmpty())
				return;

			System.out.println(Strings.join("\n", run.getErrors()));
			throw new MojoExecutionException("Test had errors");
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	private void resolve(StandaloneRun run) throws Exception, IOException {
		try (ProjectResolver projectResolver = new ProjectResolver(run)) {
			List<Container> runBundles = projectResolver.getRunBundles();
			Collection<Container> currentRunBundles = run.getRunbundles();
			Collections.sort(runBundles, new Comparator<Container>() {

				@Override
				public int compare(Container o1, Container o2) {
					return o1.getBundleSymbolicName().compareTo(o2.getBundleSymbolicName());
				}
			});

			if (!CollectionUtils.isEqualCollection(runBundles, currentRunBundles)) {
				printRunBundles(runBundles, System.out);
				if (failOnChanges) {
					throw new MojoExecutionException("The runbundles have changed. Failing the build");
				} else {
					logger.warn("The runbundles have changed:");
					run.setRunBundles(runBundles);
				}
			}
		}
	}

	private void printRunBundles(List<Container> runBundles, Appendable ps) throws IOException {
		if (runBundles.isEmpty())
			return;

		ps.append("\n-runbundles: ");
		String del = "";

		for (Container c : runBundles) {
			Version version = Version.parseVersion(c.getVersion()).getWithoutQualifier();
			VersionRange versionRange = new VersionRange(true, version,
					new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1), false);

			ps.append(del)
					.append("\\\n\t")
					.append(c.getBundleSymbolicName())
					.append("; version='")
					.append(versionRange.toString())
					.append("'");

			del = ",";
		}
		ps.append("\n\n");
	}

}
