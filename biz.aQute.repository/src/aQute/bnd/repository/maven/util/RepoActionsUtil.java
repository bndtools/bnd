package aQute.bnd.repository.maven.util;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

import org.osgi.util.promise.Promise;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Jar;
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
			final File out = new File(binary.getParentFile(), "+" + binary.getName());

			if (!out.isFile()) {
				Archive a = archive.revision.archive("jar", "sources");
				Promise<File> pSources = lookup.apply(a);

				if (pSources.getFailure() == null) {
					final File sources = pSources.getValue();

					if (sources.isFile() && sources.length() > 1000) {
						map.put("Add Sources", () -> {

							try {

								try (Jar src = new Jar(sources)) {

									try (Jar bin = new Jar(binary)) {
										bin.setDoNotTouchManifest();
										for (String path : src.getResources()
											.keySet())
											bin.putResource("OSGI-OPT/src/" + path, src.getResource(path));
										bin.write(out);
									}
									out.setLastModified(System.currentTimeMillis());
								}
							} catch (Exception e) {
								throw Exceptions.duck(e);
							}
						});
						return;
					}
				}
			}
		}

		map.put("-Add Sources", null);
	}

}
