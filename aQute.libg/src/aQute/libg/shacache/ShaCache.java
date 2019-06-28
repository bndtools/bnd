package aQute.libg.shacache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import aQute.lib.io.IO;
import aQute.lib.regex.PatternConstants;
import aQute.libg.cryptography.SHA1;

/**
 * Provide a standardized cache based on the SHA-1 of a file.
 */
public class ShaCache {
	private final static Pattern	SHA_P	= Pattern.compile(PatternConstants.SHA1);

	private final File				root;

	/**
	 * Create a SHA-1 cache on a directory.
	 *
	 * @param root the directory
	 */
	public ShaCache(File root) {
		this.root = root;
		try {
			IO.mkdirs(this.root);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot create shacache root directory " + root, e);
		}
	}

	/**
	 * Return a stream that is associated with a SHA. If the SHA is not in the
	 * local cache, the given sources parameter can specify a way to get the
	 * content.
	 *
	 * @param sha the sha
	 * @param sources objects that can retrieve the original data
	 * @return the stream or null if not found.
	 */
	public InputStream getStream(String sha, ShaSource... sources) throws Exception {

		//
		// Must be a valid SHA otherwise could be used to traverse the file
		// system
		//

		if (!SHA_P.matcher(sha)
			.matches())
			throw new IllegalArgumentException("Not a SHA");

		//
		// Get the file
		//

		File f = new File(root, sha);
		if (!f.isFile()) {

			//
			// Not found, try the sources
			//

			for (ShaSource s : sources) {
				try {
					InputStream in = s.get(sha);
					if (in == null)
						continue;

					//
					// If the source is a fast source we should
					// not cache it
					//

					if (s.isFast())
						return in;

					//
					// Create a unique temporary file
					// and copy it.
					//

					File tmp = IO.createTempFile(root, sha.toLowerCase(), ".shacache");
					IO.copy(in, tmp);
					String digest = SHA1.digest(tmp)
						.asHex();
					if (digest.equalsIgnoreCase(sha)) {

						//
						// Atomic rename. So even if it is downloaded multiple
						// times we end up with one copy and the SHA makes it
						// unique with the content.
						//

						IO.rename(tmp, f);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		//
		// Check if we succeeded
		//

		if (!f.isFile())
			return null;

		return IO.stream(f);
	}

	/**
	 * Small variation on the cache that returns a file instead of a stream
	 *
	 * @param sha the SHA-1
	 * @param sources the inputs
	 * @return a file or null
	 */
	public File getFile(String sha, ShaSource... sources) throws Exception {
		//
		// Must be a valid SHA otherwise could be used to traverse the file
		// system
		//

		if (!SHA_P.matcher(sha)
			.matches())
			throw new IllegalArgumentException("Not a SHA");

		//
		// See if we already got it
		//

		File f = new File(root, sha);
		if (f.isFile())
			return f;

		for (ShaSource s : sources) {
			try {
				InputStream in = s.get(sha);
				if (in != null) {
					File tmp = IO.createTempFile(root, sha.toLowerCase(), ".shacache");
					IO.copy(in, tmp);
					String digest = SHA1.digest(tmp)
						.asHex();
					if (digest.equalsIgnoreCase(sha)) {
						IO.rename(tmp, f);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!f.isFile())
			return null;
		return f;
	}

	/**
	 * Clean the cache
	 *
	 * @throws Exception
	 */

	public void purge() throws Exception {
		IO.deleteWithException(root);
		IO.mkdirs(root);
	}

	/**
	 * Get the root to the cache
	 */
	public File getRoot() {
		return root;
	}
}
