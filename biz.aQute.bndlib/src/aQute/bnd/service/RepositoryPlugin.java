package aQute.bnd.service;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.version.*;

public interface RepositoryPlugin {
	public enum Strategy {
		LOWEST, HIGHEST, EXACT
	}

	/**
	 * Options used to steer the put operation
	 */
	public class PutOptions {
		/**
		 * The <b>SHA1</b> digest of the artifact to put into the repository.<br/>
		 * <br/>
		 * When specified the digest of the <b>fetched</b> artifact will be
		 * calculated and verified against this digest, <b>before</b> putting
		 * the artifact into the repository.<br/>
		 * <br/>
		 * An exception is thrown if the specified digest and the calculated
		 * digest do not match.
		 */
		public byte[]	digest				= null;

		/**
		 * Allow the implementation to change the artifact.<br/>
		 * <br/>
		 * When set to true the implementation is allowed to change the artifact
		 * when putting it into the repository.<br/>
		 * <br/>
		 * An exception is thrown when set to false and the implementation can't
		 * put the artifact into the repository without changing it.
		 */
		public boolean	allowArtifactChange	= false;

		/**
		 * Generate a <b>SHA1</b> digest.<br/>
		 * <br/>
		 * When set to true the implementation generates a digest of the
		 * artifact as it is put into the repository and returns that digest in
		 * the result.
		 */
		public boolean	generateDigest		= false;

		/**
		 * Create a 'latest' artifact when it did not exist.<br/>
		 * <br/>
		 * When set to true the implementation is requested to create a 'latest'
		 * artifact.
		 */
		public boolean	createLatest		= false;
	}

	/**
	 * Results returned by the put operation
	 */
	public class PutResult {
		/**
		 * The artifact as it was put in the repository.<br/>
		 * <br/>
		 * This can be a URI to the artifact (when it was put into the
		 * repository), or null when the artifact was not put into the
		 * repository (for example because it was already in the repository).
		 */
		public URI		artifact	= null;

		/**
		 * The 'latest' artifact as it was put in the repository.<br/>
		 * <br/>
		 * Only set when {@link PutOptions#createLatest} was set to true and the
		 * 'latest' artifact did not exist, or when the 'latest' artifact did
		 * exists and was older than the artifact being put in the repository.
		 */
		public URI		latest		= null;

		/**
		 * The <b>SHA1</b> digest of the artifact as it was put into the
		 * repository.<br/>
		 * <br/>
		 * This will be null when {@link PutOptions#generateDigest} was null, or
		 * when {@link #artifact} is null.
		 */
		public byte[]	digest		= null;
	}

	/**
	 * Return a URL to a matching version of the given bundle.
	 * 
	 * @param bsn
	 *            Bundle-SymbolicName of the searched bundle
	 * @param range
	 *            Version range for this bundle,"latest" if you only want the
	 *            latest, or null when you want all.
	 * @param strategy
	 *            Get the highest or the lowest
	 * @return A list of URLs sorted on version, lowest version is at index 0.
	 *         null is returned when no files with the given bsn ould be found.
	 * @throws Exception
	 *             when anything goes wrong
	 */
	File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception;

	/**
	 * Answer if this repository can be used to store files.
	 * 
	 * @return true if writable
	 */
	boolean canWrite();

	/**
	 * Put an artifact (from the InputStream) into the repository.<br/>
	 * <br/>
	 * There is NO guarantee that the artifact on the input stream has not been
	 * modified after it's been put in the repository since that is dependent on
	 * the implementation of the repository (see
	 * {@link RepositoryPlugin.PutOptions#allowArtifactChange}).
	 * 
	 * @param stream
	 *            The input stream with the artifact
	 * @param options
	 *            The put options. See {@link RepositoryPlugin.PutOptions}
	 * @return The result of the put, never null. See
	 *         {@link RepositoryPlugin.PutResult}
	 * @throws Exception
	 *             When the repository root directory doesn't exist, when the
	 *             repository is read-only, when the specified checksum doesn't
	 *             match the checksum of the fetched artifact (see
	 *             {@link RepositoryPlugin.PutOptions#digest}), when the
	 *             implementation wants to modify the artifact but isn't allowed
	 *             (see {@link RepositoryPlugin.PutOptions#allowArtifactChange}
	 *             ), or when another error has occurred.
	 */
	PutResult put(InputStream stream, PutOptions options) throws Exception;

	/**
	 * Return a list of bsns that are present in the repository.
	 * 
	 * @param regex
	 *            if not null, match against the bsn and if matches, return
	 *            otherwise skip
	 * @return A list of bsns that match the regex parameter or all if regex is
	 *         null
	 * @throws Exception
	 */
	List<String> list(String regex) throws Exception;

	/**
	 * Return a list of versions.
	 * 
	 * @throws Exception
	 */

	List<Version> versions(String bsn) throws Exception;

	/**
	 * @return The name of the repository
	 */
	String getName();

	/**
	 * Return a location identifier of this repository
	 */

	String getLocation();
}
