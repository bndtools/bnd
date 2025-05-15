package aQute.bnd.repository.maven.util;

import static aQute.maven.api.Archive.JAR_EXTENSION;
import static aQute.maven.api.Archive.SOURCES_CLASSIFIER;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

import org.osgi.util.promise.Promise;

import aQute.maven.api.Archive;

/**
 * Common helper methods used by MavenBndRepository and BndPomRepository
 */
public class RepoActionsUtil {

	public static void addSources(final Archive archive, Function<Archive, Promise<File>> lookup,
		Map<String, Runnable> map) throws Exception {
		Promise<File> pBinary = lookup.apply(archive);

		if (pBinary.getFailure() == null) {
			final File binary = pBinary.getValue();

			map.put("Add Sources", () -> {

				Archive a = archive.getOther(JAR_EXTENSION, SOURCES_CLASSIFIER);
				lookup.apply(a);

			});
			return;
		}

	}

}
