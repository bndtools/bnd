package aQute.bnd.osgi.repository;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.resource.Resource;

import aQute.bnd.annotation.ConsumerType;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

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
	public interface FileAnalyzer {

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

	private final Set<File>	files		= new LinkedHashSet<>();
	private Path			base;
	private boolean			compress	= false;
	private String			name;
	private long			increment	= -1L;
	private FileAnalyzer	analyzer;
	private Reporter		reporter	= new Slf4jReporter(SimpleIndexer.class);

	public SimpleIndexer() {}

	/**
	 * Adds files to be indexed.
	 *
	 * @param files the files to include in the index
	 */
	public SimpleIndexer files(Collection<File> files) {
		this.files.addAll(requireNonNull(files));
		return this;
	}

	/**
	 * @param base the base URI from which the index urls are relative
	 */
	public SimpleIndexer base(URI base) {
		this.base = requireNonNull(base).getScheme()
			.equalsIgnoreCase("file")
				? new File(base).toPath()
					.toAbsolutePath()
				: null;
		return this;
	}

	/**
	 * @param compress compress with GZIP when true
	 */
	public SimpleIndexer compress(boolean compress) {
		this.compress = compress;
		return this;
	}

	/**
	 * @param name an optional name for the index
	 */
	public SimpleIndexer name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param analyzer a resource analyzer
	 */
	public SimpleIndexer analyzer(FileAnalyzer analyzer) {
		this.analyzer = analyzer;
		return this;
	}

	/**
	 * @param increment the timestamp of the index
	 */
	public SimpleIndexer increment(long increment) {
		this.increment = increment;
		return this;
	}

	/**
	 * Generate the index to the specified output stream.
	 *
	 * @param outputStream the output stream to write the index file
	 * @throws IOException if a file cannot be indexed
	 */
	public void index(OutputStream outputStream) throws IOException {
		repository().save(requireNonNull(outputStream));
	}

	/**
	 * Generate the index to the specified file.
	 *
	 * @param file the file to write the index file
	 * @throws IOException if a file cannot be indexed
	 */
	public void index(File file) throws IOException {
		repository().save(requireNonNull(file));
	}

	/**
	 * Return the resources so far.
	 *
	 * @return the set of resources handled so far.
	 */
	public List<Resource> getResources() {
		List<Resource> resources = files.stream()
			.filter(f -> f.isFile() && !f.isHidden() && f.canRead())
			.map(this::indexFile)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		return resources;
	}

	public SimpleIndexer reporter(Reporter reporter) {
		this.reporter = reporter;
		return this;
	}

	private XMLResourceGenerator repository() {
		XMLResourceGenerator repository = new XMLResourceGenerator();
		List<Resource> resources = getResources();
		repository.resources(resources);

		if (name != null) {
			repository.name(name);
		}
		if (increment > -1) {
			repository.increment(increment);
		}
		if (compress) {
			repository.compress();
		}
		return repository;
	}

	private Resource indexFile(File file) {
		try {
			ResourceBuilder resourceBuilder = new ResourceBuilder();
			if (resourceBuilder.addFile(file, relativize(file))) {
				if (analyzer != null) {
					analyzer.analyzeFile(file, resourceBuilder.safeResourceBuilder());
				}
				return resourceBuilder.build();
			}
		} catch (Exception e) {
			reporter.exception(e, "Could not index file %s", file);
		}
		return null;
	}

	private URI relativize(File file) {
		if (base == null) {
			return file.toURI();
		}
		Path filePath = file.toPath()
			.toAbsolutePath();
		Path relativePath = base.relativize(filePath);
		// Note that relativePath.toURI() gives the wrong answer for us!
		// We have to do some Windows related mashing here too :(
		String ssp = IO.normalizePath(relativePath);
		URI relativeURI;
		try {
			relativeURI = new URI(null, ssp, null);
		} catch (URISyntaxException e) {
			throw Exceptions.duck(e);
		}
		reporter.trace("Resolving %s relative to %s; Relative Path: %s, URI: %s", filePath, base, relativePath,
			relativeURI);
		return relativeURI;
	}

}
