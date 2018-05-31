/**
 * OSGi Bndrun task type for Gradle.
 *
 * <p>
 * This task type can be used to execute a bndrun file.
 *
 * <p>
 * Here is examples of using the Bndrun task type:
 * <pre>
 * import aQute.bnd.gradle.Bndrun
 * task run(type: Bndrun) {
 *   bndrun file('my.bndrun')
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the execution
 * fails. The default is false.</li>
 * <li>bndrun - This is the bndrun file to be run.
 * This property must be set.</li>
 * <li>workingDir - This is the directory for the execution.
 * The default for workingDir is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.build.Project
import aQute.bnd.build.Run
import aQute.bnd.build.Workspace
import aQute.bnd.service.RepositoryPlugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

public class Bndrun extends DefaultTask {
  /**
   * Whether execution failures should be ignored.
   *
   * <p>
   * If <code>true</code>, then execution failures will not fail the task.
   * Otherwise, a execution failure will fail the task. The default is
   * <code>false</code>.
   */
  @Input
  boolean ignoreFailures

  private File workingDir
  private File bndrun
  private final Workspace bndWorkspace

  /**
   * Create a Bndrun task.
   *
   */
  public Bndrun() {
    super()
    bndWorkspace = project.findProperty('bndWorkspace')
    ignoreFailures = false
    workingDir = temporaryDir
    if (bndWorkspace == null) {
      convention.plugins.bundles = new FileSetRepositoryConvention(this)
    }
  }

  /**
   * Return the bndrun file for the execution.
   *
   */
  @InputFile
  public File getBndrun() {
    return bndrun
  }

  /**
   * Set the bndfile for the execution.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndrun(Object file) {
    bndrun = project.file(file)
  }

  /**
   * Return the working dir for the execution.
   *
   */
  public File getWorkingDir() {
    return workingDir
  }

  /**
   * Set the working dir for the execution.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setWorkingDir(Object dir) {
    workingDir = project.file(dir)
  }

  /**
   * Execute the bndrun file.
   *
   */
  @TaskAction
  void bndrun() {
    Workspace workspace = bndWorkspace
    if (workspace != null && bndrun == project.bnd.project.getPropertiesFile()) {
      worker(project.bnd.project)
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

      worker(run)
    }
  }

  protected void worker(Project run) {
    try {
      logger.info 'Running {} in {}', run.getPropertiesFile(), run.getBase()
      run.run();
    } finally {
      logReport(run, logger)
    }
    if (!ignoreFailures && !run.isOk()) {
      throw new GradleException("${run.getPropertiesFile()} execution failure")
    }
  }
}
