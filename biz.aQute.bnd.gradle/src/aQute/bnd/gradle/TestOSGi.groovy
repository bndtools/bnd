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
 * tasks.register('testOSGi', TestOSGi) {
 *   bndrun file('my.bndrun')
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the any
 * test cases fail. Otherwise, the task will fail if any test case
 * fails. The default is false.</li>
 * <li>bndrun - This is the bndrun file to be tested.
 * This property must be set.</li>
 * <li>workingDirectory - This is the directory for the test case execution.
 * The default for workingDir is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the test case execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * <li>resultsDirectory - This is the directory
 * where the test case results are placed.
 * The default is project.testResultsDir/name.</li>
 * <li>tests - The test class names to be run.
 * If not set, all test classes are run.
 * Use a colon (:) to specify a test method to run on the specified test class.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap

import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option

public class TestOSGi extends Bndrun {
  private List<String> tests
  /**
   * The directory where the test case results are placed.
   *
   * <p>
   * The default for resultsDirectory is
   * "${buildDir}/${testResultsDirName}/${task.name}"
   */
  @OutputDirectory
  final DirectoryProperty resultsDirectory

  /**
   * Create a TestOSGi task.
   *
   */
  public TestOSGi() {
    super()
    Provider<Directory> testResultsDirectory = project.layout.buildDirectory.dir(project.provider({ ->
        return project.testResultsDirName
    }))
    resultsDirectory = project.objects.directoryProperty().convention(testResultsDirectory.map({ it.dir(name) }))
  }

  @Deprecated
  @ReplacedBy('resultsDirectory')
  public File getResultsDir() {
    return unwrap(getResultsDirectory())
  }

  /**
   * Configures the test class names to be run.
   */
  @Option(option = "tests", description = "Configures the test class names to be run.")
  public void setTests(List<String> tests) {
      this.tests = tests
  }

  /**
   * Return the test class names to be run.
   * If not set, all test classes are run.
   */
  @Input
  @Optional
  public List<String> getTests() {
      return tests;
  }

  /**
   * Test the Run object.
   */
  @Override
  protected void worker(def run) {
    logger.info 'Running tests for {} in {}', run.getPropertiesFile(), run.getBase()
    logger.debug 'Run properties: {}', run.getProperties()
    File resultsDir = unwrap(getResultsDirectory())
    try {
      run.test(resultsDir, tests);
    } finally {
      logReport(run, logger)
    }
    if (!ignoreFailures && !run.isOk()) {
      throw new GradleException("${run.getPropertiesFile()} test failure")
    }
  }
}
