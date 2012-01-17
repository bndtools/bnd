package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;

import aQute.bnd.service.*;
import aQute.bnd.service.url.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;

/**
 * <p>
 * This resource handler downloads remote resources on demand, and caches them
 * as local files. Resources that are already local (i.e. <code>file:...</code>
 * URLs) are returned directly.
 * </p>
 * 
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
 * 
 */
public class CachingURLResourceHandle implements ResourceHandle {
	
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
	
	static final String FILE_SCHEME = "file:";
	static final String HTTP_SCHEME = "http:";
	static final String UTF_8 = "UTF-8";
	
	final File cacheDir;
	final URLConnector connector;
	
	// The resolved, absolute URL of the resource
	final URL url;
	
	// The local file, if the resource IS a file, otherwise null.
	final File localFile;
	
	// The cached file copy of the resource, if it is remote and has been downloaded.
	final File cachedFile;
	
	final CachingMode mode;
	boolean downloaded = false; // only used with mode=PreferRemote
	
	Reporter reporter;
	
	public CachingURLResourceHandle(String url, String baseUrl, final File cacheDir, CachingMode mode) throws IOException {
		this(url, baseUrl, cacheDir, new DefaultURLConnector(), mode);
	}
	
	public CachingURLResourceHandle(String url, String baseUrl, final File cacheDir, URLConnector connector, CachingMode mode) throws IOException {
		this.cacheDir = cacheDir;
		this.connector = connector;
		this.mode = mode;
		
		if (url.startsWith(FILE_SCHEME)) {
			// File URL may be relative or absolute
			File file = new File(url.substring(FILE_SCHEME.length()));
			if (file.isAbsolute()) {
				this.localFile = file;
			} else {
				if (baseUrl == null || !baseUrl.startsWith(FILE_SCHEME))
					throw new IllegalArgumentException("Relative file URLs cannot be resolved if the base URL is a non-file URL.");
				this.localFile = resolveFile(baseUrl.substring(FILE_SCHEME.length()), file.toString());
			}
			this.url = localFile.toURI().toURL();
			if (!localFile.isFile() && !localFile.isDirectory())
				throw new FileNotFoundException("File URL " + this.url + " points at a non-existing file.");
			this.cachedFile = null;
		} else if (url.startsWith(HTTP_SCHEME)) {
			// HTTP URLs must be absolute
			this.url = new URL(url);
			this.localFile = null;
			this.cachedFile = mapRemoteURL(this.url);
		} else if (baseUrl == null) {
			// Some other scheme and no base => must be absolute
			this.url = new URL(url);
			this.localFile = null;
			this.cachedFile = mapRemoteURL(this.url);
		} else {
			// A path with no scheme means resolve relative to the base URL
			if (baseUrl.startsWith(FILE_SCHEME)) {
				this.localFile = resolveFile(baseUrl.substring(FILE_SCHEME.length()), url);
				this.url = localFile.toURI().toURL();
				this.cachedFile = null;
			} else {
				URL base = new URL(baseUrl);
				this.url = new URL(base, url);
				this.localFile = null;
				this.cachedFile = mapRemoteURL(this.url);
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
			throw new IllegalArgumentException("Cannot resolve relative to non-existant base file path: " + baseFileName);
		
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
			throw new IllegalStateException("Invalid URLResourceHandle: both local file and cache file location are uninitialised.");
		
		switch (mode) {
		case PreferCache:
			if (!cachedFile.exists()) {
				cacheDir.mkdirs();
				downloadToFile(url, cachedFile);
			}
			return cachedFile;
		case PreferRemote:
			File tempFile = File.createTempFile("download", ".tmp");
			try {
				downloadToFile(url, tempFile);
				
				// remote download succeeded... copy tmp to cache
				cacheDir.mkdirs();
				IO.copy(tempFile, cachedFile);
				return cachedFile;
			} catch (IOException e) {
				// Remote download failed... use the cache if available
				if (cachedFile.exists()) {
					if (reporter != null) reporter.warning("Download of remote resource %s failed, using local cache %s.", url, cachedFile); 
					return cachedFile;
				} else {
					throw new IOException(String.format("Download of remote resource %s failed and cached file %s not available!", url, cachedFile));
				}
			}
		default:
			throw new IllegalArgumentException("Invalid caching mode");
		}
	}
	
	void downloadToFile(URL url, File file) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = connector.connect(url);
			out = new FileOutputStream(file);
			
			byte[] buf = new byte[1024];
			for(;;) {
				int bytes = in.read(buf, 0, 1024);
				if (bytes < 0) break;
				out.write(buf, 0, bytes);
			}
		} finally {
			try { if (in != null) in.close(); } catch (IOException e) {};
			try { if (out != null) in.close(); } catch (IOException e) {};
		}
	}
	
	public URL getResolvedUrl() {
		return url;
	}

}
