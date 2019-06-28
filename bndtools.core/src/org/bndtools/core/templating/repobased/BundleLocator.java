package org.bndtools.core.templating.repobased;

import java.io.File;
import java.net.URI;

public interface BundleLocator {

	File locate(String bsn, String hash, String algo, URI location) throws Exception;

}
