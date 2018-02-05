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
 * }
 * task exportRunbundles(type: Export) {
 *   bndrun file('my.bndrun')
 *   bundlesOnly = true
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bundlesOnly - If true the task will export the -runbundles files
 * to desintationDir. Otherwise, an executable jar will be exported
 * to destinationDir. The default is false.</li>
 * <li>bndrun - This is the bndrun file to be resolved.
 * This property must be set.</li>
 * <li>destinationDir - This is the directory for the output.
 * The default for destinationDir is project.distsDir/'executable'
 * if bundlesOnly is false, and project.distsDir/'runbundles'/bndrun
 * if bundlesOnly is true.</li>
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
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public class Export extends DefaultTask {
  /**
   * Whether only bundles should be exported instead
   * of an executable jar.
   *
   * <p>
   * If <code>true</code>, then the -runbundles
   * will be exported. Otherwise an executable jar will be
   * exporte. The default is  <code>false</code>.
   */
  @Input
  boolean bundlesOnly

  private File bndrun
  private File destinationDir

  /**
   * Create a Export task.
   *
   */
  public Export() {
    super()
    bundlesOnly = false
    convention.plugins.bundles = new FileSetRepositoryConvention(this)
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
   * if bundlesOnly is false, and project.distsDir/'runbundles'/bndrun
   * if bundlesOnly is true.</li>
   */
  @OutputDirectory
  public File getDestinationDir() {
    if (destinationDir != null) {
      return destinationDir
    }
    if (bundlesOnly) {
      String name = bndrun.name - '.bndrun'
      destinationDir = new File(project.distsDir, "runbundles/${name}")
    } else {
      destinationDir = new File(project.distsDir, 'executable')
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
   * Export the bndrun file.
   *
   */
  @TaskAction
  void export() {
    File cnf = new File(temporaryDir, Workspace.CNFDIR)
    project.mkdir(cnf)
    Run.createRun(null, bndrun).withCloseable { Run run ->
      run.setBase(temporaryDir)
      Workspace workspace = run.getWorkspace()
      workspace.setBuildDir(cnf)
      workspace.setOffline(project.gradle.startParameter.offline)
      workspace.addBasicPlugin(getFileSetRepository(name))
      logger.info 'Exporting {} to {}', run.getPropertiesFile(), destinationDir.absolutePath
      for (RepositoryPlugin repo : workspace.getRepositories()) {
        repo.list(null)
      }
      run.getInfo(workspace)
      logReport(run, logger)
      if (!run.isOk()) {
        throw new GradleException("${run.getPropertiesFile()} standalone workspace errors")
      }

      try {
        if (bundlesOnly) {
          logger.info 'Creating a distribution of the runbundles from {} in directory {}', run.getPropertiesFile(), destinationDir.absolutePath
          def options = ['outputDir': destinationDir.absolutePath]
          run.export(RUNBUNDLES, options)
        } else {
          String name = bndrun.name - '.bndrun'
          File executableJar = new File(destinationDir, "${name}.jar")
          logger.info 'Creating an executable jar from {} to {}', run.getPropertiesFile(), executableJar.absolutePath
          def options = ['keep': 'false', 'output': executableJar.absolutePath]
          run.export(EXECUTABLE_JAR, options)
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
