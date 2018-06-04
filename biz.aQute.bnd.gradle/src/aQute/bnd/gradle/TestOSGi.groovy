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
 * task testOSGi(type: TestOSGi) {
 *   bndrun file('my.bndrun')
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the any
 * test cases fail. Otherwise, the task will fail is any test case
 * fails. The default is false.</li>
 * <li>bndrun - This is the bndrun file to be tested.
 * This property must be set.</li>
 * <li>workingDir - This is the directory for the test case execution.
 * The default for workingDir is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * <li>resultsDir (read only) - This is the directory 
 * where the test case results are placed.
 * The value is project.testResultsDir/name.</li>
 * <li>tests - The test class names to be run.
 * If not set, all test classes are run.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option

public class TestOSGi extends Bndrun {
  private List<String> tests

  /**
   * Create a TestOSGi task.
   *
   */
  public TestOSGi() {
    super()
  }

  /**
   * Return the directory where the test case results are placed.
   */
  @OutputDirectory
  public File getResultsDir() {
    return new File(project.testResultsDir, name)
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

  @Override
  protected void worker(def run) {
    try {
      logger.info 'Running tests for {} in {}', run.getPropertiesFile(), run.getBase()
      run.test(resultsDir, tests);
    } finally {
      logReport(run, logger)
    }
    if (!ignoreFailures && !run.isOk()) {
      throw new GradleException("${run.getPropertiesFile()} test failure")
    }
  }
}
