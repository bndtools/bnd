package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.testResultsDir;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.options.Option;

import aQute.bnd.build.Project;
import aQute.lib.io.IO;

/**
 * OSGi Test task type for Gradle.
 * <p>
 * This task type can be used to execute tests in a bndrun file.
 * <p>
 * Here is examples of using the TestOSGi task type:
 *
 * <pre>
 * import aQute.bnd.gradle.TestOSGi
 * tasks.register("testOSGi", TestOSGi) {
 *   bndrun = resolveTask.flatMap { it.outputBndrun }
 * }
 * </pre>
 * <p>
 * Properties:
 * <ul>
 * <li>bndrun - This is the bndrun file to be tested. This property must be
 * set.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd
 * Workspace builds. The default is "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files". This must not be used for Bnd
 * Workspace builds.</li>
 * <li>ignoreFailures - If true the task will not fail if the any test cases
 * fail. Otherwise, the task will fail if any test case fails. The default is
 * false.</li>
 * <li>workingDirectory - This is the directory for the test case execution. The
 * default for workingDir is temporaryDir.</li>
 * <li>javaLauncher - Configures the default java executable to be used for
 * execution.</li>
 * <li>resultsDirectory - This is the directory where the test case results are
 * placed. The default is project.java.testResultsDir/name.</li>
 * <li>tests - The test class names to be run. If not set, all test classes are
 * run. Use a colon (:) to specify a test method to run on the specified test
 * class.</li>
 * </ul>
 */
public class TestOSGi extends Bndrun {
	private final DirectoryProperty	resultsDirectory;
	private List<String>			tests;

	/**
	 * The directory where the test case results are placed.
	 * <p>
	 * The default for resultsDirectory is
	 * "${project.java.testResultsDir}/${task.name}"
	 *
	 * @return The directory where the test case results are placed.
	 */
	@OutputDirectory
	public DirectoryProperty getResultsDirectory() {
		return resultsDirectory;
	}

	/**
	 * Return the test class names to be run.
	 *
	 * @return The test class names to be run.
	 */
	@Input
	@Optional
	public List<String> getTests() {
		return tests;
	}

	/**
	 * Configures the test class names to be run.
	 *
	 * @param tests The test class names to be run.
	 */
	@Option(option = "tests", description = "Configures the test class names to be run.")
	public void setTests(List<String> tests) {
		this.tests = tests;
	}

	/**
	 * Create a TestOSGi task.
	 */
	public TestOSGi() {
		super();
		Provider<Directory> testResultsDir = testResultsDir(getProject());
		String taskName = getName();
		resultsDirectory = getProject().getObjects()
			.directoryProperty()
			.convention(testResultsDir.map(d -> d.dir(taskName)));
	}

	/**
	 * Test the Run object.
	 *
	 * @param run The Run object.
	 * @throws Exception If the worker action has an exception.
	 */
	@Override
	protected void worker(Project run) throws Exception {
		if (getJavaLauncher().isPresent() && Objects.equals(run.getProperty("java", "java"), "java")) {
			run.setProperty("java", IO.absolutePath(unwrapFile(unwrap(getJavaLauncher()).getExecutablePath())));
		}
		getLogger().info("Running tests for {} in {}", run.getPropertiesFile(), run.getBase());
		getLogger().debug("Run properties: {}", run.getProperties());
		File resultsDir = unwrapFile(getResultsDirectory());
		try {
			run.test(resultsDir, getTests());
		} finally {
			logReport(run, getLogger());
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException(String.format("%s test failure", run.getPropertiesFile()));
		}
	}
}
