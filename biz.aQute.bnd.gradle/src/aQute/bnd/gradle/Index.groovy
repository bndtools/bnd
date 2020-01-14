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
 * tasks.register('index', Index) {
 *   destinationDirectory = file('bundles')
 *   gzip = true
 *   bundles = fileTree(destinationDirectory) {
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
 * <li>destinationDirectory - The destination directory for the index.
 * This is used as the URI base of the generated index.
 * The default value is buildDir.</li>
 * <li>bundles - This is the bundles to be indexed. This property
 * must be set.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy
import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.osgi.repository.SimpleIndexer
import aQute.lib.io.IO
import aQute.bnd.osgi.Processor

import java.util.zip.GZIPOutputStream

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public class Index extends DefaultTask {
  private ConfigurableFileCollection bundleCollection
  private final DirectoryProperty destinationDirectory
  private final Property<URI> baseProperty
  private final Provider<RegularFile> indexUncompressed
  private final Provider<RegularFile> indexCompressed
  private String repositoryName
  private String indexName

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
  boolean gzip = false

  /**
   * Create an Index task.
   *
   */
  public Index() {
    super()
    indexName = 'index.xml'
    repositoryName = name
    bundleCollection = project.files()
    destinationDirectory = project.objects.directoryProperty().convention(project.layout.buildDirectory)
    baseProperty = project.objects.property(URI.class).convention(destinationDirectory.map({ path ->
      project.uri(path)
    }))
    indexUncompressed = destinationDirectory.file(project.provider({ ->
      return indexName
    }))
    indexCompressed = destinationDirectory.file(project.provider({ ->
      return indexName + '.gz'
    }))
    dependsOn { getBundles() }
  }

  /**
   * Get the name of the index file.
   *
   * <p>
   * The default is <code>index.xml</code>.
   */
  @Internal('Represented by indexUncompressed and indexCompressed')
  public String getIndexName() {
    return indexName
  }

  /**
   * Set the name of the index file.
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName
  }

  /**
   * Get the name attribute in the generated index.
   *
   * <p>
   * The default is the name of the task.
   */
  @Input
  public String getRepositoryName() {
    return repositoryName
  }

  /**
   * Set the name attribute in the generated index.
   */
  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName
  }

  /**
   * The destination directory for the index.
   *
   * <p>
   * The default value is buildDir.
   */
  @Internal('Represented by indexUncompressed and indexCompressed')
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
   * Return the URI base of the generated index.
   *
   * <p>
   * The default value is destinationDir.
   */
  @Input
  public Provider<URI> getBase() {
    return baseProperty
  }

  /**
   * Set the base URI directory for the index.
   *
   * <p>
   * The argument will be handled using
   * Project.uri().
   */
  public void setBase(Object path) {
    baseProperty.set(project.provider({ ->
      return project.uri(path)
    }))
  }

  /**
   * Add files to the bundles to be indexed.
   *
   * <p>
   * The arguments will be handled using
   * Project.files().
   */
  public ConfigurableFileCollection bundles(Object... paths) {
    return builtBy(bundleCollection.from(paths), paths)
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
  public Provider<RegularFile> getIndexUncompressed() {
    return indexUncompressed
  }

  /**
   * Return the compressed index file.
   */
  @OutputFile
  public Provider<RegularFile> getIndexCompressed() {
    return indexCompressed
  }

  /**
   * Index the bundles.
   *
   */
  @TaskAction
  void indexer() {
    File indexUncompressedFile = project.file(getIndexUncompressed())
    new Processor().withCloseable { Processor processor ->
      new SimpleIndexer()
        .reporter(processor)
        .files(bundles.sort())
        .base(getBase().get())
        .name(getRepositoryName())
        .index(indexUncompressedFile)

      logReport(processor, logger)
      if (!processor.isOk()) {
        failTask("Index ${indexUncompressedFile} has errors", indexUncompressedFile)
      }

      logger.info 'Generated index {}.', indexUncompressedFile
      if (gzip) {
        File indexCompressedFile = project.file(getIndexCompressed())
        indexCompressedFile.withOutputStream { out ->
          IO.copy(indexUncompressedFile, new GZIPOutputStream(out)).close()
        }
        logger.info 'Generated index {}.', indexCompressedFile
      }
    }
  }

  private void failTask(String msg, File outputFile) {
    project.delete(outputFile)
    throw new GradleException(msg)
  }
}
