/**
 * Export task type for Gradle.
 *
 * <p>
 * This task type can be used to export a bndrun file.
 *
 * <p>
 * Here is examples of using the Export task type:
 * <pre>
 * import aQute.bnd.gradle.Export
 * task exportExecutable(type: Export) {
 *   bndrun file('my.bndrun')
 *   exporter = 'bnd.executablejar'
 * }
 * task exportRunbundles(type: Export) {
 *   bndrun file('my.bndrun')
 *   exporter = 'bnd.runbundles'
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the export
 * fails. The default is false.</li>
 * <li>exporter - The name of the exporter plugin to use.
 * Bnd has two built-in exporter plugins. 'bnd.executablejar'
 * exports an executable jar and 'bnd.runbundles' exports the
 * -runbundles files. The default is 'bnd.executablejar'.</li>
 * <li>bndrun - This is the bndrun file to be exported.
 * This property must be set.</li>
 * <li>destinationDir - This is the directory for the output.
 * The default for destinationDir is project.distsDir/'executable'
 * if the exporter is 'bnd.executablejar', project.distsDir/'runbundles'/bndrun
 * if the exporter is 'bnd.runbundles', and project.distsDir/task.name
 * for all other exporters.</li>
 * <li>workingDir - This is the directory for the export operation.
 * The default for workingDir is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.exporter.executable.ExecutableJarExporter.EXECUTABLE_JAR
import static aQute.bnd.exporter.runbundles.RunbundlesExporter.RUNBUNDLES
import static aQute.bnd.gradle.BndUtils.logReport

import aQute.lib.io.IO

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

public class Export extends Bndrun {
  /**
   * This property is replaced by exporter.
   * This property is only used when the exporter
   * property is not specified.
   *
   * <p>
   * If <code>true</code>, then the exporter defaults to
   * 'bnd.runbundles'. Otherwise the expoter defaults to
   * 'bnd.executablejar'. The default is <code>false</code>.
   */
  @Input
  boolean bundlesOnly = false

  private File destinationDir
  private String exporter

  /**
   * Create a Export task.
   *
   */
  public Export() {
    super()
  }

  /**
   * Return the destination directory for the export.
   *
   * <p>
   * The default for destinationDir is project.distsDir/'executable'
   * if the exporter is 'bnd.executablejar', project.distsDir/'runbundles'/bndrun
   * if the exporter is 'bnd.runbundles', and project.distsDir/exporter
   * for all other exporters.
   */
  @OutputDirectory
  public File getDestinationDir() {
    if (destinationDir != null) {
      return destinationDir
    }
    String exporterName = getExporter()
    if (exporterName == RUNBUNDLES) {
      String bndrunName = bndrun.name - '.bndrun'
      destinationDir = new File(project.distsDir, "runbundles/${bndrunName}")
    } else if (exporterName == EXECUTABLE_JAR) {
      destinationDir = new File(project.distsDir, 'executable')
    } else {
      destinationDir = new File(project.distsDir, exporterName)
    }
    return destinationDir
  }

  /**
   * Set the destination directory for the export.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setDestinationDir(Object dir) {
    destinationDir = project.file(dir)
  }

  /**
   * Return the name of the exporter for this task.
   *
   * <p>
   * Bnd has two built-in exporter plugins. 'bnd.executablejar'
   * exports an executable jar and 'bnd.runbundles' exports the
   * -runbundles files. The default is 'bnd.executablejar' unless
   * bundlesOnly is false when the default is 'bnd.runbundles'.
   */
  @Input
  @Optional
  public String getExporter() {
    if (exporter == null) {
      exporter = bundlesOnly ? RUNBUNDLES : EXECUTABLE_JAR
    }
    return exporter
  }

  /**
   * Set the name of the exporter for this task.
   *
   * <p>
   * Bnd has two built-in exporter plugins. 'bnd.executablejar'
   * exports an executable jar and 'bnd.runbundles' exports the
   * -runbundles files.
   * <p>
   * The exporter plugin with the specified name must be an
   * installed exporter plugin.
   */
  public void setExporter(String exporter) {
    this.exporter = exporter
  }

  /**
   * Export the Run object.
   */
  @Override
  protected void worker(def run) {
    logger.info 'Exporting {} to {} with exporter {}', run.getPropertiesFile(), destinationDir, exporter
    try {
      def export = run.export(exporter, [:])
      if (exporter == RUNBUNDLES) {
        export?.value.withCloseable { jr ->
          jr.getJar().writeFolder(destinationDir)
        }
      } else {
        export?.value.withCloseable { r ->
          File exported = IO.getBasedFile(destinationDir, export.key)
          exported.withOutputStream { out ->
            r.write(out)
          }
          exported.setLastModified(r.lastModified())
        }
      }
    } finally {
      logReport(run, logger)
    }
    if (!ignoreFailures && !run.isOk()) {
      throw new GradleException("${run.getPropertiesFile()} export failure")
    }
  }
}
