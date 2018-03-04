package aQute.bnd.osgi.repository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.resource.ResourceBuilder;

public class SimpleIndexer {

	private SimpleIndexer() {
        // no instances
    }

	private static final Logger logger = LoggerFactory.getLogger(SimpleIndexer.class);

	/**
	 * Simple program to generate an index from a set of bundles.
	 *
	 * @param files the files to include in the index
	 * @param out the output stream to write the index file
	 * @param base the base URI from which the index urls are relative
	 * @param compress compress with GZIP when true
	 * @param name an optional name for the index
	 * @throws Exception if the file cannot be indexed
	 */
	public static void index(Collection<File> files, OutputStream out, URI base, boolean compress, String name)
		throws Exception {
		ResourcesRepository resourcesRepository = new ResourcesRepository();

		files.stream()
			.filter(f -> f.exists() && !f.isDirectory() && !f.isHidden() && f.canRead())
			.forEach(f -> {
				URI uri = f.toURI();
				if (base != null) {
					uri = base.relativize(uri);
				}

				try {
					ResourceBuilder resourceBuilder = new ResourceBuilder();
					if (resourceBuilder.addFile(f, uri)) {
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

		repository.save(out);
	}
}
