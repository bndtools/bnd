package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.isGradleCompatible
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap

import aQute.lib.io.IO

import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option

/**
 * OSGi Test task type for Gradle.
 *
 * <p>
 * This task type can be used to execute tests in a bndrun file.
 *
 * <p>
 * Here is examples of using the TestOSGi task type:
 * <pre>
 * import aQute.bnd.gradle.TestOSGi
 * tasks.register("testOSGi", TestOSGi) {
 *   bndrun = resolveTask.flatMap { it.outputBndrun }
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bndrun - This is the bndrun file to be tested.
 * This property must be set.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd Workspace builds. The default is
 * "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files".
 * This must not be used for Bnd Workspace builds.</li>
 * <li>ignoreFailures - If true the task will not fail if the any
 * test cases fail. Otherwise, the task will fail if any test case
 * fails. The default is false.</li>
 * <li>workingDirectory - This is the directory for the test case execution.
 * The default for workingDir is temporaryDir.</li>
 * <li>javaLauncher - Configures the default java executable to be used for execution.</li>
 * <li>resultsDirectory - This is the directory
 * where the test case results are placed.
 * The default is project.java.testResultsDir/name.</li>
 * <li>tests - The test class names to be run.
 * If not set, all test classes are run.
 * Use a colon (:) to specify a test method to run on the specified test class.</li>
 * </ul>
 */
public class TestOSGi extends Bndrun {
	/**
	 * The directory where the test case results are placed.
	 *
	 * <p>
	 * The default for resultsDirectory is
	 * "${project.java.testResultsDir}/${task.name}"
	 */
	@OutputDirectory
	final DirectoryProperty resultsDirectory
	
	/**
	 * Configures the test class names to be run.
	 */
	@Input
	@Optional
	@Option(description = "Configures the test class names to be run.")
	List<String> tests

	/**
	 * Create a TestOSGi task.
	 *
	 */
	public TestOSGi() {
		super()
		Provider<Directory> testResultsDir = isGradleCompatible("7.1") ? getProject().java.getTestResultsDir()
		: getProject().getLayout().getBuildDirectory().dir(getProject().provider(() -> getProject().testResultsDirName))
		String taskName = getName()
		resultsDirectory = getProject().getObjects().directoryProperty().convention(testResultsDir.map(d -> d.dir(taskName)))
	}

	/**
	 * Test the Run object.
	 */
	@Override
	protected void worker(var run) {
		if (getJavaLauncher().isPresent() && Objects.equals(run.getProperty("java", "java"), "java")) {
			run.setProperty("java", IO.absolutePath(getJavaLauncher().get().getExecutablePath().getAsFile()))
		}
		getLogger().info("Running tests for {} in {}", run.getPropertiesFile(), run.getBase())
		getLogger().debug("Run properties: {}", run.getProperties())
		File resultsDir = unwrap(getResultsDirectory())
		try {
			run.test(resultsDir, getTests());
		} finally {
			logReport(run, getLogger())
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException("${run.getPropertiesFile()} test failure")
		}
	}
}
