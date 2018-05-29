/**
 * Index task type for Gradle.
 *
 * <p>
 * This task type can be used to index a set of bundles.
 *
 * <p>
 * Here is an example of using the Baseline task type:
 * <pre>
 * import aQute.bnd.gradle.Index
 * task index(type: Index) {
 *   destination = file('bundles')
 *   gzip = true
 *   bundles = fileTree(destination) {
 *    include '**&#47;*.jar'
 *    exclude '**&#47;*-latest.jar'
 *    exclude '**&#47;*-sources.jar'
 *    exclude '**&#47;*-javadoc.jar'
 *  }
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>gzip - If <code>true</code>, then a gzip'd copy of the index will be made.
 * Otherwise, only the uncompressed index will be made. The default is
 * <code>false</code>.</li>
 * <li>indexName - The name of the index file. The default is
 * <code>index.xml</code>.</li>
 * <li>repositoryName - The name attribute in the generated index. The default is
 * the name of the task.</li>
 * <li>destinationDir - The destination directory for the index.
 * This is used as the URI base of the generated index.
 * The default value is buildDir.</li>
 * <li>bundles - This is the bundles to be indexed. This property
 * must be set.</li>
 * </ul>
 */

package aQute.bnd.gradle

import aQute.bnd.osgi.repository.SimpleIndexer
import aQute.lib.io.IO

import java.util.zip.GZIPOutputStream

import org.gradle.api.Buildable
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public class Index extends DefaultTask {
  private ConfigurableFileCollection bundleCollection
  private File destinationDir

  /**
   * Whether a gzip'd index should be made.
   *
   * <p>
   * If <code>true</code>, then a gzip'd copy of the index will be made.
   * Otherwise, only the uncompressed index will be made. The default is
   * <code>false</code>.
   */
  @Input
  @Optional
  boolean gzip

  /**
   * The name of the index file.
   *
   * <p>
   * The default is <code>index.xml</code>.
   */
  @Input
  @Optional
  String indexName

  /**
   * The name attribute in the generated index.
   *
   * <p>
   * The default is the name of the task.
   */
  @Input
  @Optional
  String repositoryName

  /**
   * Create an Index task.
   *
   */
  public Index() {
    super()
    gzip = false
    destinationDir = project.buildDir
    indexName = 'index.xml'
    repositoryName = name
    bundleCollection = project.files()
    dependsOn { getBundles() }
  }

  /**
   * Set the destination directory for the index.
   * This is used as the URI base of the generated index.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setDestinationDir(Object file) {
    destinationDir = project.file(file)
  }

  /**
   * Return the destination directory for the index.
   * This is used as the URI base of the generated index.
   *
   * <p>
   * The default value is buildDir.
   */
  @Input
  @Optional
  public File getDestinationDir() {
    return destinationDir
  }

  /**
   * Add files to the bundles to be indexed.
   *
   * <p>
   * The arguments will be handled using
   * Project.files().
   */
  public ConfigurableFileCollection bundles(Object... paths) {
    return bundleCollection.from(paths).builtBy(paths.findAll { path ->
      path instanceof Task || path instanceof Buildable
    })
  }

  /**
   * Get the bundles to be indexed.
   */
  @InputFiles
  public ConfigurableFileCollection getBundles() {
    return bundleCollection
  }

  /**
   * Set the bundles to be indexed.
   */
  public void setBundles(Object path) {
    bundleCollection.from = []
    bundleCollection.builtBy = []
    bundles(path)
  }

  /**
   * Return the uncompressed index file.
   */
  @OutputFile
  public File getIndexUncompressed() {
    return new File(destinationDir, indexName)
  }

  /**
   * Return the compressed index file.
   */
  @OutputFile
  public File getIndexCompressed() {
    return new File(destinationDir, indexName + '.gz')
  }

  /**
   * Index the bundles.
   *
   */
  @TaskAction
  void indexer() {
    new SimpleIndexer()
      .files(bundles.sort())
      .base(project.uri(destinationDir))
      .name(repositoryName)
      .index(indexUncompressed)
    logger.info 'Generated index {}.', indexUncompressed

    if (gzip) {
      indexCompressed.withOutputStream { out ->
        IO.copy(indexUncompressed, new GZIPOutputStream(out)).close()
      }
      logger.info 'Generated index {}.', indexCompressed
    }
  }
}
