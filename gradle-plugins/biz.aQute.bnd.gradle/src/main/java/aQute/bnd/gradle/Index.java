package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.lib.io.IO;

/**
 * Index task type for Gradle.
 * <p>
 * This task type can be used to index a set of bundles.
 * <p>
 * Here is an example of using the Index task type:
 *
 * <pre>
 * import aQute.bnd.gradle.Index
 * tasks.register("index", Index) {
 *   destinationDirectory = layout.buildDirectory.dir("libs")
 *   gzip = true
 *   bundles = fileTree(destinationDirectory) {
 *     include "**&#47;*.jar"
 *     exclude "**&#47;*-latest.jar"
 *     exclude "**&#47;*-sources.jar"
 *     exclude "**&#47;*-javadoc.jar"
 *     builtBy tasks.withType(Jar)
 *   }
 * }
 * </pre>
 * <p>
 * Properties:
 * <ul>
 * <li>base - The base URI for the generated index. The default is the file: URI
 * of the destinationDirectory. The default value is buildDir.</li>
 * <li>bundles - This is the bundles to be indexed. This property must be
 * set.</li>
 * <li>destinationDirectory - The destination directory for the index. This is
 * used as the URI base of the generated index.</li>
 * <li>gzip - If <code>true</code>, then a gzip'd copy of the index will be
 * made. Otherwise, only the uncompressed index will be made. The default is
 * <code>false</code>.</li>
 * <li>indexName - The name of the index file. The default is
 * <code>index.xml</code>.</li>
 * <li>repositoryName - The name attribute in the generated index. The default
 * is the name of the task.</li>
 * </ul>
 */
public class Index extends DefaultTask {
	private final Property<URI>					base;
	private final ConfigurableFileCollection	bundles;
	private final DirectoryProperty				destinationDirectory;
	private boolean								gzip	= false;
	private final Property<String>				indexName;
	private final Property<String>				repositoryName;
	private final RegularFileProperty			indexUncompressed;
	private final RegularFileProperty			indexCompressed;

	/**
	 * The URI base of the generated index.
	 * <p>
	 * The default is the file: URI of the destinationDir.
	 *
	 * @return The property for the base of the generated index.
	 */
	@Input
	public Property<URI> getBase() {
		return base;
	}

	/**
	 * The bundles to be indexed.
	 *
	 * @return The property for the bundles to be indexed.
	 */
	@InputFiles
	public ConfigurableFileCollection getBundles() {
		return bundles;
	}

	/**
	 * The destination directory for the index.
	 * <p>
	 * The default value is project.layout.buildDirectory.
	 *
	 * @return The property for the destination directory for the index.
	 */
	@Internal("Represented by indexUncompressed and indexCompressed")
	public DirectoryProperty getDestinationDirectory() {
		return destinationDirectory;
	}

	/**
	 * Whether a gzip'd index should be made.
	 *
	 * @return <code>true</code> if a gzip'd copy of the index will be made.
	 *         Otherwise, only the uncompressed index will be made. The default
	 *         is <code>false</code>.
	 */
	@Input
	public boolean isGzip() {
		return gzip;
	}

	/**
	 * Whether a gzip'd index should be made.
	 * <p>
	 * An alias for {@link #isGzip()}.
	 *
	 * @return <code>true</code> if a gzip'd copy of the index will be made.
	 *         Otherwise, only the uncompressed index will be made. The default
	 *         is <code>false</code>.
	 */
	@Internal
	public boolean getGzip() {
		return isGzip();
	}

	/**
	 * Set whether a gzip'd index should be made.
	 *
	 * @param gzip If <code>true</code>, then a gzip'd copy of the index will be
	 *            made. Otherwise, only the uncompressed index will be made. The
	 *            default is <code>false</code>.
	 */
	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}

	/**
	 * The name of the index file.
	 * <p>
	 * The default is <code>index.xml</code>.
	 *
	 * @return The name of the index file.
	 */
	@Internal("Represented by indexUncompressed and indexCompressed")
	public Property<String> getIndexName() {
		return indexName;
	}

	/**
	 * The name attribute in the generated index.
	 * <p>
	 * The default is the name of the task.
	 *
	 * @return The name attribute in the generated index.
	 */
	@Input
	public Property<String> getRepositoryName() {
		return repositoryName;
	}

	/**
	 * The uncompressed index file.
	 * <p>
	 * The default is <code>destinationDirectory.file(indexName)</code>.
	 *
	 * @return The uncompressed index file.
	 */
	@OutputFile
	public RegularFileProperty getIndexUncompressed() {
		return indexUncompressed;
	}

	/**
	 * The compressed index file.
	 * <p>
	 * The default is <code>destinationDirectory.file(indexName+".gz")</code>.
	 *
	 * @return The compressed index file.
	 */
	@OutputFile
	public RegularFileProperty getIndexCompressed() {
		return indexCompressed;
	}

	/**
	 * Create an Index task.
	 */
	public Index() {
		super();
		org.gradle.api.Project project = getProject();
		ObjectFactory objects = project.getObjects();
		indexName = objects.property(String.class)
			.convention("index.xml");
		repositoryName = objects.property(String.class)
			.convention(getName());
		bundles = objects.fileCollection();
		destinationDirectory = objects.directoryProperty()
			.convention(project.getLayout()
				.getBuildDirectory());
		base = objects.property(URI.class)
			.convention(destinationDirectory.map(d -> unwrapFile(d).toURI()));
		indexUncompressed = objects.fileProperty()
			.convention(destinationDirectory.file(indexName));
		indexCompressed = objects.fileProperty()
			.convention(destinationDirectory.file(indexName.map(n -> n.concat(".gz"))));
	}

	/**
	 * Add files to the bundles to be indexed.
	 *
	 * @param paths The arguments will be handled using
	 *            ConfigurableFileCollection.from().
	 * @return The property for the bundles to be indexed.
	 */
	public ConfigurableFileCollection bundles(Object... paths) {
		return builtBy(getBundles().from(paths), paths);
	}

	/**
	 * Set the bundles to be indexed.
	 *
	 * @param path The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setBundles(Object path) {
		getBundles().setFrom(Collections.emptyList());
		getBundles().setBuiltBy(Collections.emptyList());
		bundles(path);
	}

	/**
	 * Index the bundles.
	 *
	 * @throws Exception An exception during indexing.
	 */
	@TaskAction
	public void indexerAction() throws Exception {
		File indexUncompressedFile = unwrapFile(getIndexUncompressed());
		try (Processor processor = new Processor()) {
			List<File> sortedBundles = getBundles().getFiles()
				.stream()
				.sorted()
				.collect(toList());
			getLogger().info("Generating index for {}.", sortedBundles);
			new SimpleIndexer().reporter(processor)
				.files(sortedBundles)
				.base(unwrap(getBase()))
				.name(unwrap(getRepositoryName()))
				.index(indexUncompressedFile);

			logReport(processor, getLogger());
			if (!processor.isOk()) {
				failTask(String.format("Index %s has errors", indexUncompressedFile), indexUncompressedFile);
			}

			getLogger().info("Generated index {}.", indexUncompressedFile);
			if (isGzip()) {
				File indexCompressedFile = unwrapFile(getIndexCompressed());
				try (OutputStream out = new GZIPOutputStream(IO.outputStream(indexCompressedFile))) {
					IO.copy(indexUncompressedFile, out);
				}
				getLogger().info("Generated index {}.", indexCompressedFile);
			}
		}
	}

	private void failTask(String msg, File outputFile) {
		IO.delete(outputFile);
		throw new GradleException(msg);
	}
}
