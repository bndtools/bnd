package aQute.lib.deployer.obr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import aQute.bnd.service.ResourceHandle;

public class URLResourceHandle implements ResourceHandle {
	
	static final String FILE_SCHEME = "file:";
	static final String HTTP_SCHEME = "http:";
	
	final File cacheDir;
	
	// The resolved, absolute URL of the resource
	final URL url;
	
	// The local file, if the resource IS a file, otherwise null.
	final File localFile;
	
	// The cached file copy of the resource, if it is remote and has been downloaded.
	final File cachedFile;

	public URLResourceHandle(String url, String baseUrl, final File cacheDir) throws IOException {
		this.cacheDir = cacheDir;
		if (url.startsWith(FILE_SCHEME)) {
			// File URL may be relative or absolute
			File file = new File(url.substring(FILE_SCHEME.length()));
			if (file.isAbsolute()) {
				this.localFile = file;
			} else {
				if (!baseUrl.startsWith(FILE_SCHEME))
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
		String encoded = URLEncoder.encode(url.toString(), "UTF-8");
		return new File(cacheDir, encoded);
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
			throw new IllegalStateException("Invalid URLResourceHandle: both local file and cache file are uninitialised.");
		
		if (!cachedFile.exists()) {
			cacheDir.mkdirs();
			downloadToFile(url, cachedFile);
		}
		
		return cachedFile;
	}
	
	private static void downloadToFile(URL url, File file) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = url.openStream();
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

}
