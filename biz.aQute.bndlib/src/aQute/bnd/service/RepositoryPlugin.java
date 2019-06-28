package aQute.bnd.service;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.osgi.Processor;
import aQute.bnd.version.Version;

/**
 * A Repository Plugin abstract a bnd repository. This interface allows bnd to
 * find programs from their bsn and revisions from their bsn-version
 * combination. It is also possible to put revisions in a repository if the
 * repository is not read only.
 */
public interface RepositoryPlugin {
	/**
	 * Options used to steer the put operation
	 */
	class PutOptions {
		public static final String	BUNDLE	= "application/vnd.osgi.bundle";
		public static final String	LIB		= "application/vnd.aQute.lib";

		/**
		 * The <b>SHA1</b> digest of the artifact to put into the repository.
		 * When specified the digest of the <b>fetched</b> artifact will be
		 * calculated and verified against this digest, <b>before</b> putting
		 * the artifact into the repository.
		 * </p>
		 * An exception is thrown if the specified digest and the calculated
		 * digest do not match.
		 */
		public byte[]				digest	= null;

		/**
		 * Specify the mime type of the importing stream. This can be either
		 * {@link #BUNDLE} or {@link #LIB}. If left open, it is up to the
		 * repository to guess the content type.
		 */
		public String				type;

		/**
		 * When set, the repository must use it as the bsn
		 */
		public String				bsn		= null;

		/**
		 * When set, the repository must use it as the version
		 */
		public Version				version	= null;

		/**
		 * Provides the context. This is an optional parameter but if possible
		 * should link to the closest context of the dumped artifact. It will be
		 * used for reporting and getting properties/instructions.
		 */
		public Processor			context;
	}

	PutOptions DEFAULTOPTIONS = new PutOptions();

	/**
	 * Results returned by the put operation
	 */
	class PutResult {
		/**
		 * A (potentially public) uri to the revision as it was put in the
		 * repository.<br/>
		 * <br/>
		 * This can be a URI to the given artifact (when it was put into the
		 * repository). This does not have to be a File URI!
		 */
		public URI		artifact	= null;

		/**
		 * The <b>SHA1</b> digest of the artifact as it was put into the
		 * repository.<br/>
		 * <br/>
		 * This can be null and it can differ from the input digest if the
		 * repository rewrote the stream for optimization reason. If the
		 */
		public byte[]	digest		= null;

		/**
		 * Set to true if this artifact was already released
		 */
		public boolean	alreadyReleased;
	}

	/**
	 * Put an artifact (from the InputStream) into the repository.<br/>
	 * <br/>
	 * There is <b>no guarantee</b> that the artifact on the input stream has
	 * not been modified after it's been put in the repository since that is
	 * dependent on the implementation of the repository.
	 *
	 * @param stream The input stream with the artifact
	 * @param options The put options. See {@link RepositoryPlugin.PutOptions},
	 *            can be {@code null}, which will then take the default options
	 *            like new PutOptions().
	 * @return The result of the put, never null. See
	 *         {@link RepositoryPlugin.PutResult}
	 * @throws Exception When the repository root directory doesn't exist, when
	 *             the repository is read-only, when the specified checksum
	 *             doesn't match the checksum of the fetched artifact (see
	 *             {@link RepositoryPlugin.PutOptions#digest}), when the
	 *             implementation wants to modify the artifact but isn't
	 *             allowed, or when another error has occurred.
	 */
	PutResult put(InputStream stream, PutOptions options) throws Exception;

	/**
	 * The caller can specify any number of DownloadListener objects that are
	 * called back when a download is finished (potentially before the get
	 * method has returned).
	 */

	interface DownloadListener {
		/**
		 * Called when the file is successfully downloaded from a remote
		 * repository.
		 *
		 * @param file The file that was downloaded
		 * @throws Exception , are logged and ignored
		 */
		void success(File file) throws Exception;

		/**
		 * Called when the file could not be downloaded from a remote
		 * repository.
		 *
		 * @param file The file that was intended to be downloaded.
		 * @throws Exception , are logged and ignored
		 */
		void failure(File file, String reason) throws Exception;

		/**
		 * Can be called back regularly before success/failure but never after.
		 * Indicates how far the download has progressed in percents. Since
		 * downloads can be restarted, it is possible that the percentage
		 * decreases.
		 *
		 * @param file The file that was intended to be downloaded
		 * @param percentage Percentage of file downloaded (can go down)
		 * @return true if the download should continue, fails if it should be
		 *         canceled (and fail)
		 * @throws Exception , are logged and ignored
		 */
		boolean progress(File file, int percentage) throws Exception;
	}

	/**
	 * Return a URL to a matching version of the given bundle.
	 * <p/>
	 * If download listeners are specified then the returned file is not
	 * guaranteed to exist before a download listener is notified of success or
	 * failure. The callback can happen before the method has returned. If the
	 * returned file is null then download listeners are not called back.
	 * <p/>
	 * The intention of the Download Listeners is to allow a caller to obtain
	 * references to files that do not yet exist but are to be downloaded. If
	 * the downloads were done synchronously in the call, then no overlap of
	 * downloads could take place.
	 *
	 * @param bsn Bundle-SymbolicName of the searched bundle
	 * @param version Version requested
	 * @param listeners Zero or more download listener that will be notified of
	 *            the outcome.
	 * @return A file to the revision or null if not found
	 * @throws Exception when anything goes wrong, in this case no listeners
	 *             will be called back.
	 */
	File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception;

	/**
	 * Answer if this repository can be used to store files.
	 *
	 * @return true if writable
	 */
	boolean canWrite();

	/**
	 * Return a list of bsns that are present in the repository.
	 *
	 * @param pattern A
	 *            <ahref="https://en.wikipedia.org/wiki/Glob_%28programming%29">
	 *            glob pattern</a> to be matched against bsns present in the
	 *            repository, or {@code null}.
	 * @return A list of bsns that match the pattern parameter or all if pattern
	 *         is null; repositories that do not support browsing or querying
	 *         should return an empty list.
	 * @throws Exception
	 */
	List<String> list(String pattern) throws Exception;

	/**
	 * Return a list of versions.
	 *
	 * @throws Exception
	 */

	SortedSet<Version> versions(String bsn) throws Exception;

	/**
	 * @return The name of the repository
	 */
	String getName();

	/**
	 * Return a location identifier of this repository
	 */

	String getLocation();
}
