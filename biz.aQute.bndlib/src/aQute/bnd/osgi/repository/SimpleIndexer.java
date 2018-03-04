package aQute.bnd.osgi.repository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ConsumerType;
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
		 * add zero or more capabilities and/or requirements to the supplied
		 * lists.
		 * </p>
		 * <p>
		 * The method <b>may</b> examine the lists of already-discovered
		 * requirements and capabilities; for example they may wish to add a
		 * certain capability if (and only if) it has not already been added.
		 * </p>
		 * <p>
		 * The method <b>MUST NOT</b> attempt to remove or replace any
		 * capability or requirement from the supplied list. Clients of this
		 * method may enforce this by passing List implementations that throw
		 * {@link UnsupportedOperationException} upon any attempt to call
		 * {@link List#remove(int)}, etc.
		 * </p>
		 *
		 * @param file The current file.
		 * @param capabilities The list of capabilities.
		 * @param requirements The list of requirements.
		 */
		void analyzeFile(File file, List<Capability> capabilities, List<Requirement> requirements) throws Exception;
	}

	private static final Logger				logger		= LoggerFactory.getLogger(SimpleIndexer.class);

	/**
	 * @param files the files to include in the index
	 * @param out the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream out, URI base) throws Exception {
		index(files, out, base, false, null, null);
	}

	/**
	 * @param files the files to include in the index
	 * @param out the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @param compress compress with GZIP when true
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream out, URI base, boolean compress) throws Exception {
		index(files, out, base, compress, null, null);
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
		index(files, out, base, compress, name, null);
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
		FileAnalyzer analyzer)
		throws Exception {

		Objects.requireNonNull(files, "'files' argument cannot be null");
		Objects.requireNonNull(outputStream, "'outputStream' argument cannot be null");
		Objects.requireNonNull(base, "'base' argument cannot be null");

		ResourcesRepository resourcesRepository = new ResourcesRepository();

		files.stream()
			.filter(f -> f.exists() && !f.isDirectory() && !f.isHidden() && f.canRead())
			.forEach(f -> {
				URI uri = base.relativize(f.toURI());

				try {
					ResourceBuilder resourceBuilder = new ResourceBuilder();
					if (resourceBuilder.addFile(f, uri)) {
						if (analyzer != null) {
							analyzer.analyzeFile(f, resourceBuilder.getCapabilities(),
								resourceBuilder.getRequirements());
						}
						resourcesRepository.add(resourceBuilder.build());
					}
				} catch (Exception e) {
					logger.error("Could not index file {}", f, e);
				}
			});

		XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();
		XMLResourceGenerator repository = xmlResourceGenerator.repository(resourcesRepository);

		if (name != null) {
			repository.name(name);
		}

		if (compress) {
			repository.compress();
		}

		repository.save(outputStream);
	}
}
