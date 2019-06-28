package aQute.libg.shacache;

import java.io.InputStream;

/**
 * An object that can retrieve an inputstream on a given SHA-1
 */
public interface ShaSource {
	/**
	 * Retrieving the stream is fast so do not cache
	 *
	 * @return true if a fast retrieval can be done
	 */
	boolean isFast();

	/**
	 * Get an inputstream based on the given SHA-1
	 *
	 * @param sha the SHA-1
	 * @return a stream or null if not found
	 */
	InputStream get(String sha) throws Exception;
}
