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
 * 'configurations.archives.artifacts.files'</li>
 * <li>resultsDir (read only) - This is the directory 
 * where the test case results are placed.
 * The value is project.testResultsDir/name</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.build.Run
import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import aQute.bnd.repository.fileset.FileSetRepository
import aQute.bnd.service.RepositoryPlugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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
  private ConfigurableFileCollection bundles

  /**
   * Create a TestOSGi task.
   *
   */
  public TestOSGi() {
    super()
    ignoreFailures = false
    workingDir = temporaryDir
    bundles = project.files(project.sourceSets.main.runtimeClasspath, project.configurations.archives.artifacts.files)
    dependsOn project.assemble
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
   * Add files to use when locating bundles for the test execution.
   *
   * <p>
   * The arguments will be handled using
   * Project.files().
   */
  public ConfigurableFileCollection bundles(Object... paths) {
    return bundles.from(paths)
  }

  /**
   * Return the files to use when locating bundles for the test execution.
   */
  @InputFiles
  public ConfigurableFileCollection getBundles() {
    return bundles
  }

  /**
   * Set the files to use when locating bundles for the test execution.
   *
   * <p>
   * The argument will be handled using
   * Project.files().
   */
  public void setBundles(Object path) {
   bundles = project.files(path)
  }

  /**
   * Return the directory where the test case results are placed.
   */
  @OutputDirectory
  public File getResultsDir() {
    return new File(project.testResultsDir, name)
  }

  /**
   * Test the bndrun file.
   *
   */
  @TaskAction
  void testOSGi() {
    project.mkdir(workingDir)
    File cnf = new File(temporaryDir, Workspace.CNFDIR)
    project.mkdir(cnf)
    Run.createRun(null, bndrun).withCloseable { run ->
      run.setBase(workingDir)
      Workspace workspace = run.getWorkspace()
      workspace.setBuildDir(cnf)
      workspace.setOffline(project.gradle.startParameter.offline)
      workspace.addBasicPlugin(new FileSetRepository(name, bundles.files))
      logger.info 'Running tests for {} in {}', run.getPropertiesFile(), workingDir.absolutePath
      for (RepositoryPlugin repo : workspace.getRepositories()) {
        repo.list(null)
      }
      run.getInfo(workspace)
      logReport(run, logger)
      if (!run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} standalone workspace errors")
      }

      try {
        run.test(resultsDir, null);
      } finally {
        logReport(run, logger)
      }
      if (!ignoreFailures && !run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} test failure")
      }
    }
  }
}
