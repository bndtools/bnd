package org.osgi.service.indexer.impl;

import java.io.File;
import java.net.URI;

/**
 * Override the calculation of the URL with a specific function.
 */
public interface URLResolver {

	/**
	 * Calculate the URL for the given artifact. If this returns null or throws
	 * an exception, the automatic calculation will be used. Exceptions are
	 * logged so should not be used for flow control.
	 * 
	 * @param artifact The artifact being analyzed
	 * @return Either a URI to be used in the content capability or null if the
	 *         default method should be used
	 */
	URI resolver(File artifact) throws Exception;
}
