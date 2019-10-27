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

import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledExecutorService

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Processor

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
  boolean ignoreFailures = false

  private File workingDir
  private File bndrun
  private final def bndWorkspace

  /**
   * Create a Bndrun task.
   *
   */
  public Bndrun() {
    super()
    bndWorkspace = project.findProperty('bndWorkspace')
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
    return workingDir ?: temporaryDir
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
   * Setup the Run object and call worker on it.
   */
  @TaskAction
  void bndrun() {
    def workspace = bndWorkspace
    if ((workspace != null) && project.plugins.hasPlugin(BndPlugin.PLUGINID) && (bndrun == project.bnd.project.getPropertiesFile())) {
      worker(project.bnd.project)
      return
    }
    createRun(workspace, bndrun).withCloseable { run ->
      def runWorkspace = run.getWorkspace()
      project.mkdir(workingDir)
      if (workspace == null) {
        Properties gradleProperties = new PropertiesWrapper(runWorkspace.getProperties())
        gradleProperties.put('task', this)
        gradleProperties.put('project', project)
        run.setParent(new Processor(runWorkspace, gradleProperties, false))
      }
      run.setBase(workingDir)
      if (run.isStandalone()) {
        runWorkspace.setOffline(workspace != null ? workspace.isOffline() : project.gradle.startParameter.offline)
        File cnf = new File(workingDir, Workspace.CNFDIR)
        project.mkdir(cnf)
        runWorkspace.setBuildDir(cnf)
        if (convention.findPlugin(FileSetRepositoryConvention)) {
          runWorkspace.addBasicPlugin(getFileSetRepository(name))
          runWorkspace.getRepositories().each { repo ->
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

  /**
   * Create the Run object.
   */
  protected def createRun(def workspace, File bndrun) {
    Class runClass = workspace ? Class.forName(aQute.bnd.build.Run.class.getName(), true, workspace.getClass().getClassLoader()) : aQute.bnd.build.Run.class
    return runClass.createRun(workspace, bndrun)
  }

  /**
   * Execute the Run object.
   */
  protected void worker(def run) {
    logger.info 'Running {} in {}', run.getPropertiesFile(), run.getBase()
    ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    try {
      run.getProjectLauncher().withCloseable() { pl ->
        pl.liveCoding(ForkJoinPool.commonPool(), scheduledExecutor).withCloseable() {
          pl.setTrace(run.isTrace() || run.isRunTrace())
          pl.launch()
        }
      }
    } finally {
      scheduledExecutor.shutdownNow()
      logReport(run, logger)
    }
    if (!ignoreFailures && !run.isOk()) {
      throw new GradleException("${run.getPropertiesFile()} execution failure")
    }
  }
}
