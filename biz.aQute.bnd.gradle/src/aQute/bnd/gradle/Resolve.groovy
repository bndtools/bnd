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
 * def resolveTask = tasks.register('resolve', Resolve) {
 *   bndrun = file('my.bndrun')
 *   outputBndrun = layout.buildDirectory.file('my.bndrun')
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the execution
 * fails. The default is false.</li>
 * <li>failOnChanges - If true the task will fail if the resolve process
 * results in a different value for -runbundles than the current value.
 * The default is false.</li>
 * <li>writeOnChanges - If true the task will write changes to the value
 * of the -runbundles property. The default is true.</li>
 * <li>bndrun - This is the bndrun file to be resolved.
 * This property must be set.</li>
 * <li>outputBndrun - This is an optional output file for the calculated
 * -runbundles property. This property is optional, and if not set, the
 * input bndrun file will be updated in place.</li>
 * <li>workingDirectory - This is the directory for the resolve process.
 * The default for workingDirectory is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the resolve process. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * <li>reportOptional - If true failure reports will include
 * optional requirements. The default is true.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap

import aQute.bnd.osgi.Constants
import aQute.lib.io.IO
import aQute.lib.utf8properties.UTF8Properties
import biz.aQute.resolve.ResolveProcess

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

import org.osgi.service.resolver.ResolutionException

public class Resolve extends Bndrun {
  /**
   * Whether resolve changes should fail the task.
   *
   * <p>
   * If <code>true</code>, then a change to the current -runbundles
   * value will fail the task. The default is
   * <code>false</code>.
   */
  @Input
  boolean failOnChanges = false

  /**
   * Whether resolve changes should be writen back.
   *
   * <p>
   * If <code>true</code>, then a change to the current -runbundles
   * value will be writen back into the bndrun file. The default is
   * <code>true</code>.
   */
  @Input
  boolean writeOnChanges = true

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

  /**
   * Return the optional output file for the calculated `-runbundles`
   * property.
   *
   * <p>
   * This output file will -include the input bndrun file and can be
   * thus be used by other tasks, such as TestOSGi as a resolved
   * input bndrun file.
   */
  @OutputFile
  @Optional
  final RegularFileProperty outputBndrun

  /**
   * Create a Resolve task.
   */
  public Resolve() {
    super()
    outputBndrun = project.objects.fileProperty()
  }

  /**
   * Create the Bndrun object.
   */
  protected def createRun(def workspace, File bndrunFile) {
    File outputBndrunFile = unwrap(getOutputBndrun(), true)
    if (outputBndrunFile != null) {
      outputBndrunFile.withWriter('UTF-8') { writer ->
        UTF8Properties props = new UTF8Properties()
        props.setProperty(Constants.INCLUDE, String.format('"%s"', IO.absolutePath(bndrunFile)))
        props.store(writer, null)
      }
      bndrunFile = outputBndrunFile
    }
    Class runClass = workspace ? Class.forName(biz.aQute.resolve.Bndrun.class.getName(), true, workspace.getClass().getClassLoader()) : biz.aQute.resolve.Bndrun.class
    return runClass.createBndrun(workspace, bndrunFile)
  }

  /**
   * Resolve the Bndrun object.
   */
  protected void worker(def run) {
    logger.info 'Resolving runbundles required for {}', run.getPropertiesFile()
    logger.debug 'Run properties: {}', run.getProperties()
    try {
      def result = run.resolve(failOnChanges, writeOnChanges)
      logger.info '{}: {}', Constants.RUNBUNDLES, result
    } catch (ResolutionException e) {
      logger.error ResolveProcess.format(e, reportOptional)
      throw new GradleException("${run.getPropertiesFile()} resolution exception", e)
    } finally {
      logReport(run, logger)
    }
    if (!ignoreFailures && !run.isOk()) {
      throw new GradleException("${run.getPropertiesFile()} resolution failure")
    }
  }
}
