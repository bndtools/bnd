package aQute.bnd.deployer.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.url.URLConnector;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.service.reporter.Reporter;

/**
 * <p>
 * This resource handler downloads remote resources on demand, and caches them
 * as local files. Resources that are already local (i.e. <code>file:...</code>
 * URLs) are returned directly.
 * </p>
 * <p>
 * Two alternative caching modes are available. When the mode is
 * {@link CachingMode#PreferCache}, the cached file will always be returned if
 * it exists; therefore to refresh from the remote resource it will be necessary
 * to delete the cache. When the mode is {@link CachingMode#PreferRemote}, the
 * first call to {@link #request()} will always attempt to download the remote
 * resource, and only uses the pre-downloaded cache if the remote could not be
 * downloaded (e.g. because the network is offline).
 * </p>
 *
 * @author njbartlett
 */
public class CachingUriResourceHandle implements ResourceHandle {
	static final int			BUFFER_SIZE	= IOConstants.PAGE_SIZE * 1;

	private static final String	SHA_256		= "SHA-256";

	@Deprecated
	public enum CachingMode {
		/**
		 * Always use the cached file, if it exists.
		 */
		@Deprecated
		PreferCache,

		/**
		 * Download the remote resource if possible, falling back to the cached
		 * file if remote fails. Subsequently the cached resource will be used.
		 */
		@Deprecated
		PreferRemote;
	}

	static final String	FILE_SCHEME	= "file";
	static final String	FILE_PREFIX	= FILE_SCHEME + ":";

	static final String	HTTP_SCHEME	= "http";
	static final String	HTTP_PREFIX	= HTTP_SCHEME + ":";
	static final String	UTF_8		= "UTF-8";

	final File			cacheDir;
	final URLConnector	connector;

	// The resolved, absolute URL of the resource
	final URL			url;
	String				sha;

	// The local file, if the resource IS a file, otherwise null.
	final File			localFile;

	// The cached file copy of the resource, if it is remote and has been
	// downloaded.
	final File			cachedFile;
	final File			shaFile;

	final CachingMode	mode;

	Reporter			reporter;

	public CachingUriResourceHandle(URI uri, final File cacheDir, URLConnector connector, String sha)
		throws IOException {
		this.cacheDir = cacheDir;
		this.connector = connector;
		this.mode = CachingMode.PreferRemote;
		this.sha = sha;

		if (!uri.isAbsolute())
			throw new IllegalArgumentException("Relative URIs are not permitted " + uri);

		if (FILE_SCHEME.equals(uri.getScheme())) {
			this.localFile = new File(uri.getPath());
			this.url = uri.toURL();
			this.cachedFile = null;
			this.shaFile = null;
		} else {
			this.url = uri.toURL();
			this.localFile = null;
			this.cachedFile = mapRemoteURL(url);
			this.shaFile = mapSHAFile(cachedFile);
		}
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	static File resolveFile(String baseFileName, String fileName) {
		File resolved;

		File baseFile = new File(baseFileName);
		if (baseFile.isDirectory())
			resolved = new File(baseFile, fileName);
		else if (baseFile.isFile())
			resolved = new File(baseFile.getParentFile(), fileName);
		else
			throw new IllegalArgumentException(
				"Cannot resolve relative to non-existant base file path: " + baseFileName);

		return resolved;
	}

	private File mapRemoteURL(URL url) throws UnsupportedEncodingException, IOException {

		String localDirName;
		String localFileName;

		String fullUrl = url.toExternalForm();
		int lastSlashIndex = fullUrl.lastIndexOf('/');

		File localDir;
		if (lastSlashIndex > -1) {
			localDirName = URLEncoder.encode(fullUrl.substring(0, lastSlashIndex), UTF_8);
			localDir = new File(cacheDir, localDirName);
			if (localDir.exists() && !localDir.isDirectory()) {
				localDir = cacheDir;
				localFileName = URLEncoder.encode(fullUrl, UTF_8);
			} else {
				localFileName = URLEncoder.encode(fullUrl.substring(lastSlashIndex + 1), UTF_8);
			}
		} else {
			localDir = cacheDir;
			localFileName = URLEncoder.encode(fullUrl, UTF_8);
		}
		IO.mkdirs(localDir);

		return new File(localDir, localFileName);
	}

	private static File mapSHAFile(File cachedFile) {
		return new File(cachedFile.getAbsolutePath() + AbstractIndexedRepo.REPO_INDEX_SHA_EXTENSION);
	}

	@Override
	public String getName() {
		return url.toString();
	}

	@Override
	public Location getLocation() {
		Location result;

		if (localFile != null)
			result = Location.local;
		else if (cachedFile.exists())
			result = Location.remote_cached;
		else
			result = Location.remote;

		return result;
	}

	@Override
	public File request() throws Exception {
		if (localFile != null)
			return localFile;
		if (cachedFile == null)
			throw new IllegalStateException(
				"Invalid URLResourceHandle: both local file and cache file location are uninitialised.");

		// Check whether the cached copy exist and has the right SHA.
		boolean cacheExists = cachedFile.isFile();
		boolean cacheValidated = false;
		if (cacheExists && sha != null) {
			String cachedSHA = getCachedSHA();
			cacheValidated = sha.equalsIgnoreCase(cachedSHA);
		}

		if (cacheValidated)
			return cachedFile;

		try (InputStream data = connector.connect(url)) {

			// Save the data to the cache
			ensureCacheDirExists();
			String serverSHA = copyWithSHA(data, IO.outputStream(cachedFile));

			// Check the SHA of the received data
			if (sha != null && !sha.equalsIgnoreCase(serverSHA)) {
				IO.delete(shaFile);
				IO.delete(cachedFile);
				throw new IOException(String.format("Invalid SHA on remote resource at %s", url));
			}
			saveSHAFile(serverSHA);

			return cachedFile;
		} catch (IOException | HttpRequestException e) {
			if (sha == null) {
				// Remote access failed, use the cache if it exists AND if the
				// original SHA was not known.
				if (cacheExists) {
					if (reporter != null)
						reporter.warning("Using local cache; downloading %s failed (%s).", url, e);
					return cachedFile;
				} else {
					if (reporter != null)
						reporter.error("Downloading %s failed (%s) and cache file %s is not available. Trace: %s", url,
							e, cachedFile, collectStackTrace(e));
					throw new IOException(
						String.format("Downloading %s failed and cache file %s is not available, see log for details.",
							url, cachedFile));
				}
			} else {
				// Can only get here if the cache was missing or didn't match
				// the SHA, and remote access failed.
				if (reporter != null)
					reporter.error(
						"Downloading %s failed (%s) and cache file %s is not available or doesn't match the expected checksum. Trace: %s",
						url, e, cachedFile, collectStackTrace(e));
				throw new IOException(String.format(
					"Downloading %s failed and cache file %s is not available or doesn't match the expected checksum, see log for details.",
					url, cachedFile));
			}
		}
	}

	private String copyWithSHA(InputStream input, OutputStream output) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance(SHA_256);
			DigestOutputStream digestOutput = new DigestOutputStream(output, digest);
			IO.copy(input, digestOutput);
			return Hex.toHexString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			// Can't happen... hopefully...
			throw new IOException(e.getMessage(), e);
		} finally {
			IO.close(input);
			IO.close(output);
		}
	}

	private void ensureCacheDirExists() throws IOException {
		if (cacheDir.isDirectory())
			return;

		if (cacheDir.exists()) {
			String message = String.format(
				"Cannot create cache directory in path %s: the path exists but is not a directory",
				cacheDir.getCanonicalPath());
			if (reporter != null)
				reporter.error(message);
			throw new IOException(message);
		}

		try {
			IO.mkdirs(cacheDir);
		} catch (IOException e) {
			if (reporter != null) {
				String message = String.format("Failed to create cache directory in path %s",
					cacheDir.getCanonicalPath());
				reporter.exception(e, message);
			}
			throw e;
		}
	}

	private static String collectStackTrace(Throwable t) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream pps = new PrintStream(buffer, false, UTF_8);
			t.printStackTrace(pps);
			return buffer.toString(UTF_8);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	String getCachedSHA() throws IOException {
		String content = readSHAFile();
		if (content == null) {
			content = calculateSHA(cachedFile);
			if (content != null) {
				saveSHAFile(content);
			}
		}
		return content;
	}

	static String calculateSHA(File file) throws IOException {
		if (file == null || !file.exists()) {
			return null;
		}

		try {
			MessageDigest digest = MessageDigest.getInstance(SHA_256);
			IO.copy(file, digest);
			return Hex.toHexString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			// Can't happen... hopefully...
			throw new IOException(e.getMessage(), e);
		}
	}

	String readSHAFile() throws IOException {
		String result;
		if (shaFile != null && shaFile.isFile())
			result = IO.collect(shaFile);
		else
			result = null;
		return result;
	}

	void saveSHAFile(String contents) {
		try {
			IO.store(contents, shaFile);
		} catch (IOException e) {
			IO.delete(shaFile);
			// Errors saving the SHA should not interfere with the download
			if (reporter != null)
				reporter.exception(e, "Failed to save SHA file %s (%s)", shaFile, e);
		}
	}

}
