package aQute.bnd.osgi.metainf;

import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.merge.MergeFiles;

/**
 * Knows how to "merge" duplicate files in META-INF/services, by concatenating
 * them with a linebreak in between.
 */
public class MetaInfServiceMerger implements MergeFiles {

	@Override
	public Optional<Resource> merge(String path, Resource a, Resource b) {

		if (!path.startsWith("META-INF/services/")) {
			return Optional.empty();
		}

		// do something with a and b
		try (SequenceInputStream in = new SequenceInputStream(Collections.enumeration(
			Arrays.asList(a.openInputStream(), new ByteArrayInputStream("\n".getBytes()), b.openInputStream())));) {

			long lastModified = Math.max(a.lastModified(), b.lastModified());
			Resource r = new EmbeddedResource(ByteBuffer.wrap(in.readAllBytes()), lastModified);

			return Optional.of(r);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}


	}

}
