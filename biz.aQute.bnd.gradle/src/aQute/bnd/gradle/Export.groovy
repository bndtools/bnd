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
 * <li>exporter - The name of the exporter plugin to use.
 * Bnd has two built-in exporter plugins. 'bnd.executablejar'
 * exports an executable jar and 'bnd.runbundles' exports the
 * -runbundles files. The default is 'bnd.executablejar'.</li>
 * <li>bndrun - This is the bndrun file to be resolved.
 * This property must be set.</li>
 * <li>destinationDir - This is the directory for the output.
 * The default for destinationDir is project.distsDir/'executable'
 * if the exporter is 'bnd.executablejar', project.distsDir/'runbundles'/bndrun
 * if the exporter is 'bnd.runbundles', and project.distsDir/task.name
 * for all other exporters.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.exporter.executable.ExecutableJarExporter.EXECUTABLE_JAR
import static aQute.bnd.exporter.runbundles.RunbundlesExporter.RUNBUNDLES
import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.build.Run
import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import aQute.bnd.service.RepositoryPlugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public class Export extends DefaultTask {
  /**
   * This property is replace by exporter.
   * This property is only used when the exporter
   * property is not specified.
   *
   * <p>
   * If <code>true</code>, then the exporter defaults to
   * 'bnd.runbundles'. Otherwise the expoter defaults to
   * 'bnd.executablejar'. The default is  <code>false</code>.
   */
  @Input
  boolean bundlesOnly

  private File bndrun
  private File destinationDir
  private String exporter
  private final Workspace bndWorkspace

  /**
   * Create a Export task.
   *
   */
  public Export() {
    super()
    bndWorkspace = project.findProperty('bndWorkspace')
    bundlesOnly = false
    if (bndWorkspace == null) {
      convention.plugins.bundles = new FileSetRepositoryConvention(this)
    }
  }

  /**
   * Return the bndrun file to be exported.
   *
   */
  @InputFile
  public File getBndrun() {
    return bndrun
  }

  /**
   * Set the bndfile to be exported.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndrun(Object file) {
    bndrun = project.file(file)
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
   * Export the bndrun file.
   *
   */
  @TaskAction
  void export() {
    Workspace workspace = bndWorkspace
    Run.createRun(workspace, bndrun).withCloseable { Run run ->
      Workspace runWorkspace = run.getWorkspace()
      run.setBase(temporaryDir)
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

      try {
        logger.info 'Exporting {} to {} with exporter {}', run.getPropertiesFile(), destinationDir, exporter
        def export = run.export(exporter, [:])
        if (exporter == RUNBUNDLES) {
          export?.value.withCloseable { jr ->
            jr.getJar().writeFolder(destinationDir)
          }
        } else {
          export?.value.withCloseable { r ->
            File exported = new File(destinationDir, export.key)
            exported.withOutputStream { out ->
              r.write(out)
            }
            exported.setLastModified(r.lastModified())
          }
        }
      } finally {
        logReport(run, logger)
      }
      if (!run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} export failure")
      }
    }
  }
}
