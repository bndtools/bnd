/**
 * Index task type for Gradle.
 *
 * <p>
 * This task type can be used to index a set of bundles.
 *
 * <p>
 * Here is an example of using the Index task type:
 * <pre>
 * import aQute.bnd.gradle.Index
 * tasks.register('index', Index) {
 *   destinationDirectory = layout.buildDirectory.dir('libs')
 *   gzip = true
 *   bundles = fileTree(destinationDirectory) {
 *     include '**&#47;*.jar'
 *     exclude '**&#47;*-latest.jar'
 *     exclude '**&#47;*-sources.jar'
 *     exclude '**&#47;*-javadoc.jar'
 *     builtBy tasks.withType(Jar)
 *   }
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
 * <li>base - The base URI for the generated index. The default is
 * the file: URI of the destinationDirectory.</li>
 * The default value is buildDir.</li>
 * <li>bundles - This is the bundles to be indexed. This property
 * must be set.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap
import static java.util.stream.Collectors.toList

import aQute.bnd.osgi.repository.SimpleIndexer
import aQute.bnd.osgi.Processor
import aQute.lib.io.IO

import java.util.zip.GZIPOutputStream

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public class Index extends DefaultTask {
	/**
	 * Whether a gzip'd index should be made.
	 *
	 * <p>
	 * If <code>true</code>, then a gzip'd copy of the index will be made.
	 * Otherwise, only the uncompressed index will be made. The default is
	 * <code>false</code>.
	 */
	@Input
	boolean gzip = false

	/**
	 * The URI base of the generated index.
	 *
	 * <p>
	 * The default is the file: URI of the destinationDir.
	 */
	@Input
	final Property<URI> base

	/**
	 * The name attribute in the generated index.
	 *
	 * <p>
	 * The default is the name of the task.
	 */
	@Input
	final Property<String> repositoryName

	/**
	 * The name of the index file.
	 *
	 * <p>
	 * The default is <code>index.xml</code>.
	 */
	@Internal('Represented by indexUncompressed and indexCompressed')
	final Property<String> indexName

	/**
	 * The destination directory for the index.
	 *
	 * <p>
	 * The default value is project.layout.buildDirectory.
	 */
	@Internal('Represented by indexUncompressed and indexCompressed')
	final DirectoryProperty destinationDirectory

	/**
	 * The uncompressed index file.
	 *
	 * <p>
	 * The default is <code>destinationDirectory.file(indexName)</code>.
	 */
	@OutputFile
	final RegularFileProperty indexUncompressed

	/**
	 * The compressed index file.
	 *
	 * <p>
	 * The default is <code>destinationDirectory.file(indexName+".gz")</code>.
	 */
	@OutputFile
	final RegularFileProperty indexCompressed

	/**
	 * The bundles to be indexed.
	 */
	@InputFiles
	final ConfigurableFileCollection bundles

	/**
	 * Create an Index task.
	 *
	 */
	public Index() {
		super()
		ObjectFactory objects = getProject().getObjects()
		indexName = objects.property(String.class).convention('index.xml')
		repositoryName = objects.property(String.class).convention(getName())
		bundles = objects.fileCollection()
		destinationDirectory = objects.directoryProperty().convention(getProject().getLayout().getBuildDirectory())
		base = objects.property(URI.class).convention(destinationDirectory.map(d -> d.getAsFile().toURI()))
		indexUncompressed = objects.fileProperty().convention(destinationDirectory.file(indexName))
		indexCompressed = objects.fileProperty().convention(destinationDirectory.file(indexName.map(n -> n.concat('.gz'))))
	}

	@Deprecated
	@ReplacedBy('destinationDirectory')
	public File getDestinationDir() {
		return unwrap(getDestinationDirectory())
	}

	@Deprecated
	public void setDestinationDir(Object dir) {
		getDestinationDirectory().set(getProject().file(dir))
	}

	/**
	 * Add files to the bundles to be indexed.
	 *
	 * <p>
	 * The arguments will be handled using
	 * ConfigurableFileCollection.from().
	 */
	public ConfigurableFileCollection bundles(Object... paths) {
		return builtBy(getBundles().from(paths), paths)
	}

	/**
	 * Set the bundles to be indexed.
	 */
	public void setBundles(Object path) {
		getBundles().setFrom(Collections.emptyList())
		getBundles().setBuiltBy(Collections.emptyList())
		bundles(path)
	}

	/**
	 * Index the bundles.
	 *
	 */
	@TaskAction
	void indexerAction() {
		File indexUncompressedFile = unwrap(getIndexUncompressed())
		try (Processor processor = new Processor()) {
			var sortedBundles = getBundles().getFiles()
			.stream()
			.sorted()
			.collect(toList())
			getLogger().info('Generating index for {}.', sortedBundles)
			new SimpleIndexer()
			.reporter(processor)
			.files(sortedBundles)
			.base(unwrap(getBase()))
			.name(unwrap(getRepositoryName()))
			.index(indexUncompressedFile)

			logReport(processor, getLogger())
			if (!processor.isOk()) {
				failTask("Index ${indexUncompressedFile} has errors", indexUncompressedFile)
			}

			getLogger().info('Generated index {}.', indexUncompressedFile)
			if (isGzip()) {
				File indexCompressedFile = unwrap(getIndexCompressed())
				try (OutputStream out = new GZIPOutputStream(IO.outputStream(indexCompressedFile))) {
					IO.copy(indexUncompressedFile, out)
				}
				getLogger().info('Generated index {}.', indexCompressedFile)
			}
		}
	}

	private void failTask(String msg, File outputFile) {
		IO.delete(outputFile)
		throw new GradleException(msg)
	}
}
