/**
 * Resolve task type for Gradle.
 *
 * <p>
 * This task type can be used to resolve a bndrun file
 * setting the `-runbundles` instruction.
 *
 * <p>
 * Here is an example of using the Resolve task type:
 * <pre>
 * import aQute.bnd.gradle.Resolve
 * task resolve(type: Resolve) {
 *   bndrun file('my.bndrun')
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>failOnChanges - If true the task will fail if the resolve process
 * results in a different value for -runbundles than the current value.
 * The default is false.</li>
 * <li>bndrun - This is the bndrun file to be resolved.
 * This property must be set.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * <li>reportOptional - If true failure reports will include
 * optional requirements. The default is true.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import biz.aQute.resolve.Bndrun
import biz.aQute.resolve.ResolveProcess

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

import org.osgi.service.resolver.ResolutionException

public class Resolve extends DefaultTask {
  /**
   * Whether resolve changes should fail the task.
   *
   * <p>
   * If <code>true</code>, then a change to the current -runbundles
   * value will fail the task. The default is
   * <code>false</code>.
   */
  @Input
  boolean failOnChanges

  /**
   * Whether to report optional requirements.
   *
   * <p>
   * If <code>true</code>, optional requirements will be reported. The
   * default is <code>true</code>.
   *
   */
  @Input
  boolean reportOptional = true

  private File bndrun
  private final def bndWorkspace

  /**
   * Create a Resolve task.
   *
   */
  public Resolve() {
    super()
    bndWorkspace = project.findProperty('bndWorkspace')
    failOnChanges = false
    if (bndWorkspace == null) {
      convention.plugins.bundles = new FileSetRepositoryConvention(this)
    }
  }

  /**
   * Return the bndrun file to be resolved.
   *
   */
  @InputFile
  public File getBndrun() {
    return bndrun
  }

  /**
   * Set the bndfile to be resolved.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndrun(Object file) {
    bndrun = project.file(file)
  }

  /**
   * Resolve the bndrun file.
   *
   */
  @TaskAction
  void resolve() {
    def workspace = bndWorkspace
    Class runClass = workspace ? Class.forName(Bndrun.class.getName(), true, workspace.getClass().getClassLoader()) : Bndrun.class
    runClass.createBndrun(workspace, bndrun).withCloseable { run ->
      def runWorkspace = run.getWorkspace()
      run.setBase(temporaryDir)
      if (run.isStandalone()) {
        runWorkspace.setOffline(workspace != null ? workspace.isOffline() : project.gradle.startParameter.offline)
        File cnf = new File(temporaryDir, Workspace.CNFDIR)
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

      try {
        logger.info 'Resolving runbundles required for {}', run.getPropertiesFile()
        def result = run.resolve(failOnChanges, true)
        logger.info '{}: {}', Constants.RUNBUNDLES, result
      } catch (ResolutionException e) {
        logger.error ResolveProcess.format(e, reportOptional)
        throw new GradleException("${run.getPropertiesFile()} resolution exception", e)
      } finally {
        logReport(run, logger)
      }
      if (!run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} resolution failure")
      }
    }
  }
}
