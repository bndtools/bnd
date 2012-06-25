package aQute.lib.deployer.repository;

import java.io.*;
import java.net.*;

import aQute.bnd.service.*;
import aQute.bnd.service.url.*;
import aQute.lib.deployer.http.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;

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

	public static enum CachingMode {
		/**
		 * Always use the cached file, if it exists.
		 */
		PreferCache,

		/**
		 * Download the remote resource if possible, falling back to the cached
		 * file if remote fails. Subsequently the cached resource will be used.
		 */
		PreferRemote;
	}

	static final String FILE_SCHEME = "file";
	static final String	FILE_PREFIX	= FILE_SCHEME + ":";
	
	static final String HTTP_SCHEME = "http";
	static final String	HTTP_PREFIX	= HTTP_SCHEME + ":";
	static final String	UTF_8		= "UTF-8";

	final File			cacheDir;
	final URLConnector	connector;

	// The resolved, absolute URL of the resource
	final URL			url;

	// The local file, if the resource IS a file, otherwise null.
	final File			localFile;

	// The cached file copy of the resource, if it is remote and has been
	// downloaded.
	final File			cachedFile;

	// The etag file stores the etag of the last downloaded version
	final File			etagFile;

	final CachingMode	mode;

	Reporter			reporter;

	public CachingUriResourceHandle(URI uri, File cacheDir, CachingMode mode) throws IOException {
		this(uri, cacheDir, new DefaultURLConnector(), mode);
	}
	
	public CachingUriResourceHandle(URI uri, final File cacheDir, URLConnector connector, CachingMode mode) throws IOException {
		this.cacheDir = cacheDir;
		this.connector = connector;
		this.mode = mode;

		if (!uri.isAbsolute())
			throw new IllegalArgumentException("Relative URIs are not permitted.");
		
		if (FILE_SCHEME.equals(uri.getScheme())) {
			this.localFile = new File(uri.getPath());
			this.url = uri.toURL();
			this.cachedFile = null;
			this.etagFile = null;
		} else {
			this.url = uri.toURL();
			this.localFile = null;
			this.cachedFile = mapRemoteURL(url);
			this.etagFile = mapETag(cachedFile);
		}
	}

	public CachingUriResourceHandle(String url, URI baseUrl, final File cacheDir, URLConnector connector, CachingMode mode) throws IOException {
		this.cacheDir = cacheDir;
		this.connector = connector;
		this.mode = mode;

		if (url.startsWith(FILE_PREFIX)) {
			// File URL may be relative or absolute
			File file = new File(url.substring(FILE_PREFIX.length()));
			if (file.isAbsolute()) {
				this.localFile = file;
			} else {
				if (baseUrl == null || !FILE_SCHEME.equals(baseUrl.getScheme()))
					throw new IllegalArgumentException(
							"Relative file URLs cannot be resolved if the base URL is a non-file URL.");
				this.localFile = resolveFile(baseUrl.getPath(), file.toString());
			}
			this.url = localFile.toURI().toURL();
			if (!localFile.isFile() && !localFile.isDirectory())
				throw new FileNotFoundException("File URL " + this.url + " points at a non-existing file.");
			this.cachedFile = null;
			this.etagFile = null;
		} else if (url.startsWith(HTTP_PREFIX)) {
			// HTTP URLs must be absolute
			this.url = new URL(url);
			this.localFile = null;
			this.cachedFile = mapRemoteURL(this.url);
			this.etagFile = mapETag(cachedFile);
		} else if (baseUrl == null) {
			// Some other scheme and no base => must be absolute
			this.url = new URL(url);
			this.localFile = null;
			this.cachedFile = mapRemoteURL(this.url);
			this.etagFile = mapETag(cachedFile);
		} else {
			// A path with no scheme means resolve relative to the base URL
			if (FILE_SCHEME.equals(baseUrl.getScheme())) {
				this.localFile = resolveFile(baseUrl.getPath(), url);
				this.url = localFile.toURI().toURL();
				this.cachedFile = null;
				this.etagFile = null;
			} else {
				URL base = baseUrl.toURL();
				this.url = new URL(base, url);
				this.localFile = null;
				this.cachedFile = mapRemoteURL(this.url);
				this.etagFile = mapETag(cachedFile);
			}
		}
	}
	
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	File resolveFile(String baseFileName, String fileName) {
		File resolved;

		File baseFile = new File(baseFileName);
		if (baseFile.isDirectory())
			resolved = new File(baseFile, fileName);
		else if (baseFile.isFile())
			resolved = new File(baseFile.getParentFile(), fileName);
		else
			throw new IllegalArgumentException("Cannot resolve relative to non-existant base file path: "
					+ baseFileName);

		return resolved;
	}

	private File mapRemoteURL(URL url) throws UnsupportedEncodingException {

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
		localDir.mkdirs();

		return new File(localDir, localFileName);
	}

	private File mapETag(File cachedFile) {
		return new File(cachedFile.getAbsolutePath() + ".etag");
	}

	public String getName() {
		return url.toString();
	}

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

	public File request() throws IOException {
		if (localFile != null)
			return localFile;
		if (cachedFile == null)
			throw new IllegalStateException(
					"Invalid URLResourceHandle: both local file and cache file location are uninitialised.");

		switch (mode) {
			case PreferCache :
				if (!cachedFile.exists()) {
					try {
						TaggedData data = connector.connectTagged(url);

						// Save the etag
						if (data.getTag() != null)
							saveETag(data.getTag());

						// Download to the cache
						cacheDir.mkdirs();
						IO.copy(data.getInputStream(), cachedFile);
					}
					catch (IOException e) {
						if (reporter != null)
							reporter.error(
									"Download of remote resource %s failed and cache file %s not available. Original exception: %s. Trace: %s",
									url, cachedFile, e, collectStackTrace(e));
						throw new IOException(
								String.format(
										"Download of remote resource %s failed and cache file %s not available, see log for details.",
										url, cachedFile));
					}
				}
				return cachedFile;
			case PreferRemote :
				boolean cacheExists = cachedFile.exists();
				String etag = readETag();

				try {
					// Only send the etag if we have a cached copy corresponding
					// to that etag!
					TaggedData data = cacheExists ? connector.connectTagged(url, etag) : connector.connectTagged(url);

					// Null return means the cached file is still current
					if (data == null)
						return cachedFile;

					// Save the etag...
					if (data.getTag() != null)
						saveETag(data.getTag());

					// Save the data to the cache
					cacheDir.mkdirs();
					IO.copy(data.getInputStream(), cachedFile);
					return cachedFile;
				}
				catch (IOException e) {
					// Remote access failed, use the cache if available
					if (cacheExists) {
						if (reporter != null)
							reporter.warning(
									"Download of remote resource %s failed, using local cache %s. Original exception: %s. Trace: %s",
									url, cachedFile, e, collectStackTrace(e));
						return cachedFile;
					} else {
						if (reporter != null)
							reporter.error(
									"Download of remote resource %s failed and cache file %s not available. Original exception: %s. Trace: %s",
									url, cachedFile, e, collectStackTrace(e));
						throw new IOException(
								String.format(
										"Download of remote resource %s failed and cache file %s not available, see log for details.",
										url, cachedFile));
					}
				}
			default :
				throw new IllegalArgumentException("Invalid caching mode");
		}
	}

	private String collectStackTrace(Throwable t) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream pps = new PrintStream(buffer, false, "UTF-8");
			t.printStackTrace(pps);
			return buffer.toString("UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	String readETag() throws IOException {
		String etag;
		if (etagFile != null && etagFile.isFile())
			etag = IO.collect(etagFile);
		else
			etag = null;
		return etag;
	}

	void saveETag(String etag) {
		try {
			IO.copy(IO.stream(etag), etagFile);
		}
		catch (Exception e) {
			// Errors saving the etag should not interfere with the download
			if (reporter != null)
				reporter.error("Failed to save ETag file %s (%s)", etagFile, e.getMessage());
		}
	}

}
