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
 * <li>bndrun - This is the bndrun file to be resolved.
 * This property must be set.</li>
 * <li>workingDir - This is the directory for the test case execution.
 * The default for destinationDir is temporaryDir.</li>
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

import aQute.bnd.build.Run
import aQute.bnd.build.Project
import aQute.bnd.build.Workspace
import aQute.bnd.service.RepositoryPlugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

public class TestOSGi extends DefaultTask {
  /**
   * Whether test execution failures should be ignored.
   *
   * <p>
   * If <code>true</code>, then test execution failures will not fail the task.
   * Otherwise, a test execution failure will fail the task. The default is
   * <code>false</code>.
   */
  @Input
  boolean ignoreFailures

  private File workingDir
  private File bndrun
  private List<String> tests
  private final Workspace bndWorkspace

  /**
   * Create a TestOSGi task.
   *
   */
  public TestOSGi() {
    super()
    bndWorkspace = project.findProperty('bndWorkspace')
    ignoreFailures = false
    workingDir = temporaryDir
    if (bndWorkspace == null) {
      convention.plugins.bundles = new FileSetRepositoryConvention(this)
    }
    project.check.dependsOn this
  }

  /**
   * Return the bndrun file for the test execution.
   *
   */
  @InputFile
  public File getBndrun() {
    return bndrun
  }

  /**
   * Set the bndfile for the test execution.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndrun(Object file) {
    bndrun = project.file(file)
  }

  /**
   * Return the working dir for the test execution.
   *
   */
  public File getWorkingDir() {
    return workingDir
  }

  /**
   * Set the working dir for the test execution.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setWorkingDir(Object dir) {
    workingDir = project.file(dir)
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

  /**
   * Test the bndrun file.
   *
   */
  @TaskAction
  void testOSGi() {
    Workspace workspace = bndWorkspace
    if (workspace != null && bndrun == project.bnd.project.getPropertiesFile()) {
      testWorker(project.bnd.project)
      return
    }
    Run.createRun(workspace, bndrun).withCloseable { Run run ->
      Workspace runWorkspace = run.getWorkspace()
      project.mkdir(workingDir)
      run.setBase(workingDir)
      if (run.isStandalone()) {
        runWorkspace.setOffline(workspace != null ? workspace.isOffline() : project.gradle.startParameter.offline)
        File cnf = new File(temporaryDir, Workspace.CNFDIR)
        project.mkdir(cnf)
        runWorkspace.setBuildDir(cnf)
        if (convention.findPlugin(FileSetRepositoryConvention)) {
          runWorkspace.addBasicPlugin(getFileSetRepository(name))
          for (RepositoryPlugin repo : runWorkspace.getRepositories()) {
            repo.list(null)
          }
        }
      }
      run.getInfo(runWorkspace)
      logReport(run, logger)
      if (!run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} workspace errors")
      }

      testWorker(run)
    }
  }

  void testWorker(Project run) {
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
