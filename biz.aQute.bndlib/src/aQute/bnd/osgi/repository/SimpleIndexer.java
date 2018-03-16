package aQute.bnd.osgi.repository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ConsumerType;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Simple program to generate an index from a set of bundles.
 */
public class SimpleIndexer {

	/**
	 * A functional interface providing an entry point for performing additional
	 * analysis of indexed files.
	 */
	@ConsumerType
	@FunctionalInterface
	public static interface FileAnalyzer {

		/**
		 * <p>
		 * This method is invoked for each file being indexed. Implementations
		 * may inspect the requirements and capabilities already assembled from
		 * the file. They may add zero or more capabilities and/or requirements
		 * to the supplied resource builder possibly extracted as additional
		 * metadata from the file.
		 * </p>
		 * <p>
		 * The following operations on {@link ResourceBuilder resourceBuilder}
		 * are reduced to a no-op:
		 * <ul>
		 * <li>{@link ResourceBuilder#build()} does nothing, returns null</li>
		 * <li>{@link ResourceBuilder#addFile(File, URI)} does nothing, returns
		 * false</li>
		 * <li>{@link ResourceBuilder#addManifest(Domain)} does nothing, returns
		 * false</li>
		 * <li>{@link ResourceBuilder#getCapabilities()} returns immutable
		 * list</li>
		 * <li>{@link ResourceBuilder#getRequirements()} returns immutable
		 * list</li>
		 * </ul>
		 *
		 * @param file The current file
		 * @param resourceBuilder the resource builder used to process the file
		 */
		void analyzeFile(File file, ResourceBuilder resourceBuilder) throws Exception;
	}

	private static final Logger logger = LoggerFactory.getLogger(SimpleIndexer.class);

	/**
	 * @param files the files to include in the index
	 * @param out the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream out, URI base) throws Exception {
		index(files, out, base, false, null, -1, null);
	}

	/**
	 * @param files the files to include in the index
	 * @param out the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @param compress compress with GZIP when true
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream out, URI base, boolean compress) throws Exception {
		index(files, out, base, compress, null, -1, null);
	}

	/**
	 * @param files the files to include in the index
	 * @param out the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @param compress compress with GZIP when true
	 * @param name an optional name for the index
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream out, URI base, boolean compress, String name)
		throws Exception {
		index(files, out, base, compress, name, -1, null);
	}

	/**
	 * @param files the files to include in the index
	 * @param outputStream the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @param compress compress with GZIP when true
	 * @param name an optional name for the index
	 * @param analyzer a resource analyzer
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream outputStream, URI base, boolean compress, String name,
		FileAnalyzer analyzer) throws Exception {

		index(files, outputStream, base, compress, name, -1, analyzer);
	}

	/**
	 * @param files the files to include in the index
	 * @param outputStream the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @param compress compress with GZIP when true
	 * @param name an optional name for the index
	 * @param increment the timestamp of the index
	 * @param analyzer a resource analyzer
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream outputStream, URI base, boolean compress, String name,
		long increment, FileAnalyzer analyzer) throws Exception {

		Objects.requireNonNull(files, "'files' argument cannot be null");
		Objects.requireNonNull(outputStream, "'outputStream' argument cannot be null");
		Objects.requireNonNull(base, "'base' argument cannot be null");

		ResourcesRepository resourcesRepository = files.stream()
			.filter(f -> f.exists() && !f.isDirectory() && !f.isHidden() && f.canRead())
			.map(f -> {
				try {
					ResourceBuilder resourceBuilder = new ResourceBuilder();
					if (resourceBuilder.addFile(f, base.relativize(f.toURI()))) {
						if (analyzer != null) {
							analyzer.analyzeFile(f, resourceBuilder.safeResourceBuilder());
						}
						return resourceBuilder.build();
					}
				} catch (Exception e) {
					logger.error("Could not index file {}", f, e);
				}
				return null;
			})
			.collect(ResourcesRepository.toResourcesRepository());

		XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();
		XMLResourceGenerator repository = xmlResourceGenerator.repository(resourcesRepository);

		if (name != null) {
			repository.name(name);
		}

		if (increment > -1) {
			repository.increment(increment);
		}

		if (compress) {
			repository.compress();
		}

		repository.save(outputStream);
	}
}
