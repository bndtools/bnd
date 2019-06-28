package org.bndtools.core.templating.repobased;

import java.io.File;
import java.net.URI;

import aQute.lib.io.IO;

// TODO need to use some kind of cache to avoid repeated downloads
public class DirectDownloadBundleLocator implements BundleLocator {

	@Override
	public File locate(String bsn, String hash, String algo, URI location) throws Exception {
		File tempFile = File.createTempFile("download", "jar");
		tempFile.deleteOnExit();

		IO.copy(location.toURL(), tempFile);
		return tempFile;
	}

}
