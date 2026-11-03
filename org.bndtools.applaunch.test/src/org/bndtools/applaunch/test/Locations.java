package org.bndtools.applaunch.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.osgi.service.datalocation.Location;

/**
 * Helpers shared by the integration tests.
 */
final class Locations {
	static final String	APPLICATION	= "org.bndtools.applaunch.test.application";
	static final String	PRODUCT		= "org.bndtools.applaunch.test.product";
	static final String	PRODUCT_NAME	= "Applaunch Test Product";

	private Locations() {}

	/** The canonical file system path of a location, {@code null} if unset. */
	static String path(Location location) throws IOException {
		if (!location.isSet()) {
			return null;
		}
		return path(location.getURL());
	}

	/** The canonical file system path of a file URL, with '/' as separator. */
	static String path(URL url) throws IOException {
		File file;
		try {
			file = new File(new URI(url.toExternalForm()).getPath());
		} catch (URISyntaxException e) {
			// Equinox may produce file URLs with unescaped characters
			file = new File(url.getPath());
		}
		return file.getCanonicalPath()
			.replace(File.separatorChar, '/');
	}
}
