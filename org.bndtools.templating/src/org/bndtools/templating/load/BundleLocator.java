package org.bndtools.templating.load;

import java.io.File;

public interface BundleLocator {
	
	File locate(String bsn, String hash, String algo) throws Exception;
	
}
