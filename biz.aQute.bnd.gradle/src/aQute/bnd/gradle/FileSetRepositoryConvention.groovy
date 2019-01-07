/**
 * Task convention to make a FileSetRepository from
 * a bundles property.
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy

import aQute.bnd.repository.fileset.FileSetRepository

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles


class FileSetRepositoryConvention {
  private final Project project
  private final ConfigurableFileCollection bundleCollection

  /**
   * Create a FileSetRepositoryConvention.
   *
   */
  FileSetRepositoryConvention(Task task) {
    this.project = task.project
    bundleCollection = project.files()
    bundles(project.sourceSets.main.runtimeClasspath, project.configurations.archives.artifacts.files)
    // need to programmatically add to inputs since @InputFiles in a convention is not processed
    task.inputs.files(bundleCollection).withPropertyName('bundles')
  }

  /**
   * Add files to use when locating bundles.
   *
   * <p>
   * The arguments will be handled using
   * Project.files().
   */
  public ConfigurableFileCollection bundles(Object... paths) {
    return builtBy(bundleCollection.from(paths), paths)
  }

  /**
   * Return the files to use when locating bundles.
   */
  @InputFiles
  public ConfigurableFileCollection getBundles() {
    return bundleCollection
  }

  /**
   * Set the files to use when locating bundles.
   *
   * <p>
   * The argument will be handled using
   * Project.files().
   */
  public void setBundles(Object path) {
    bundleCollection.from = []
    bundleCollection.builtBy = []
    bundles(path)
  }

  /**
   * Return a FileSetRepository using the bundles.
   */
  FileSetRepository getFileSetRepository(String name) {
    return new FileSetRepository(name, bundles.files)
  }
}
