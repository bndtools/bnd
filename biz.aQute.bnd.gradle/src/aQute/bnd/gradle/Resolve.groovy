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
 * 'configurations.archives.artifacts.files'</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import aQute.bnd.service.RepositoryPlugin
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

  private File bndrun

  /**
   * Create a Resolve task.
   *
   */
  public Resolve() {
    super()
    failOnChanges = false
    convention.plugins.bundles = new FileSetRepositoryConvention(this)
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
    File cnf = new File(temporaryDir, Workspace.CNFDIR)
    project.mkdir(cnf)
    Bndrun.createBndrun(null, bndrun).withCloseable { Bndrun run ->
      run.setBase(temporaryDir)
      Workspace workspace = run.getWorkspace()
      workspace.setBuildDir(cnf)
      workspace.setOffline(project.gradle.startParameter.offline)
      workspace.addBasicPlugin(getFileSetRepository(name))
      logger.info 'Resolving runbundles required for {}', run.getPropertiesFile()
      for (RepositoryPlugin repo : workspace.getRepositories()) {
        repo.list(null)
      }
      run.getInfo(workspace)
      logReport(run, logger)
      if (!run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} standalone workspace errors")
      }

      try {
        def result = run.resolve(failOnChanges, true)
        logger.info '{}: {}', Constants.RUNBUNDLES, result
      } catch (ResolutionException e) {
        logger.error 'Unresolved requirements: {}', ResolveProcess.format(e.getUnresolvedRequirements())
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
