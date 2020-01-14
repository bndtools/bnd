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
 * tasks.register('exportExecutable', Export) {
 *   bndrun file('my.bndrun')
 *   exporter = 'bnd.executablejar'
 * }
 * tasks.register('exportRunbundles', Export) {
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
 * <li>destinationDirectory - This is the directory for the output.
 * The default for destinationDirectory is project.distsDirectory.dir('executable')
 * if the exporter is 'bnd.executablejar', project.distsDirectory.dir('runbundles'/bndrun)
 * if the exporter is 'bnd.runbundles', and project.distsDirectory.dir(task.name)
 * for all other exporters.</li>
 * <li>workingDirectory - This is the directory for the export operation.
 * The default for workingDirectory is temporaryDir.</li>
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
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
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

  private final DirectoryProperty destinationDirectory
  private final Property<String> exporterProperty

  /**
   * Create a Export task.
   *
   */
  public Export() {
    super()
    exporterProperty = project.objects.property(String.class).convention(project.provider({ ->
      return bundlesOnly ? RUNBUNDLES : EXECUTABLE_JAR
    }))
    Provider<Directory> distsDirectory = project.hasProperty('distsDirectory') ? project.distsDirectory : // Gradle 6.0
      project.layout.buildDirectory.dir(project.provider({ ->
        return project.distsDirName
      }))
    destinationDirectory = project.objects.directoryProperty().convention(distsDirectory.flatMap({ distsDir ->
      return distsDir.dir(getExporter().map({ exporterName ->
        if (exporterName == EXECUTABLE_JAR) {
          return 'executable'
        }
        if (exporterName == RUNBUNDLES) {
          File bndrunFile = project.file(getBndrun())
          String bndrunName = bndrunFile.name - '.bndrun'
          return "runbundles/${bndrunName}"
        }
        return exporterName
      }))
    }))
  }

  /**
   * Return the destination directory for the export.
   *
   * <p>
   * The default for destinationDirectory is project.distsDirectory.dir('executable')
   * if the exporter is 'bnd.executablejar', project.distsDirectory.dir('runbundles'/bndrun)
   * if the exporter is 'bnd.runbundles', and project.distsDirectory.dir(task.name)
   * for all other exporters.
   */
  @OutputDirectory
  public DirectoryProperty getDestinationDirectory() {
    return destinationDirectory
  }

  @Deprecated
  @ReplacedBy('destinationDirectory')
  public File getDestinationDir() {
    return project.file(getDestinationDirectory())
  }

  @Deprecated
  public void setDestinationDir(Object dir) {
    getDestinationDirectory().set(project.file(dir))
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
  public Property<String> getExporter() {
    return exporterProperty
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
    exporterProperty.convention(exporter).set(exporter)
  }

  /**
   * Export the Run object.
   */
  @Override
  protected void worker(def run) {
    String exporterName = getExporter().get()
    File destinationDirFile = project.file(getDestinationDirectory())
    logger.info 'Exporting {} to {} with exporter {}', run.getPropertiesFile(), destinationDirFile, exporterName
    try {
      def export = run.export(exporterName, [:])
      if (exporterName == RUNBUNDLES) {
        export?.value.withCloseable { jr ->
          jr.getJar().writeFolder(destinationDirFile)
        }
      } else {
        export?.value.withCloseable { r ->
          File exported = IO.getBasedFile(destinationDirFile, export.key)
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
