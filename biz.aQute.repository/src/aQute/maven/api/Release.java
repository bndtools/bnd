package aQute.maven.api;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This object can be used to release a revision. The actual work will be done
 * when this object is closed. This makes it convenient to do this in a
 * try/resource block.
 */
@ProviderType
public interface Release extends Closeable {

	/**
	 * Set this release to abort
	 */
	void abort();

	/**
	 * Add an archive
	 */
	void add(Archive archive, InputStream in) throws Exception;

	/**
	 * Add an archive, a copy of the file is made
	 */
	void add(Archive archive, File in) throws Exception;

	/**
	 * Add an archive
	 */
	void add(String extension, String classifier, InputStream in) throws Exception;

	/**
	 * Set the replacement for the SNAPSHOT part
	 *
	 * @throws Exception
	 */
	void setBuild(long timestamp, String build) throws Exception;

	/**
	 * Set the replacement for the SNAPSHOT part
	 */
	void setBuild(String timestamp, String build);

	/**
	 * Ensure that no remote update is done
	 */
	void setLocalOnly();

	/**
	 * Force an overwrite even if the artifact exists
	 */
	void force();

	/**
	 * Set a pass phrase and indicate that the files must be signed.
	 *
	 * @param passphrase the passphrase
	 */
	void setPassphrase(String passphrase);
}
