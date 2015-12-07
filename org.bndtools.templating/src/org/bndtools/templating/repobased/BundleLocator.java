package org.bndtools.templating.repobased;

import java.io.File;

public interface BundleLocator {
	
	File locate(String bsn, String hash, String algo) throws Exception;
	
}
