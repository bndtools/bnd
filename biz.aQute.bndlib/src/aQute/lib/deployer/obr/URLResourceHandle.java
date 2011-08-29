package aQute.lib.deployer.obr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import aQute.bnd.service.ResourceHandle;

public class URLResourceHandle implements ResourceHandle {
	
	static final String FILE_SCHEME = "file:";
	static final String HTTP_SCHEME = "http:";
	
	// The resolved, absolute URL of the resource
	final URL url;
	
	// The local file, if the resource IS a file, otherwise null.
	final File localFile;
	
	// The cached file copy of the resource, if it is remote and has been downloaded.
	File cachedFile = null;

	public URLResourceHandle(String url, String baseUrl, File cacheDir) throws IOException {
		if (url.startsWith(FILE_SCHEME)) {
			// File URL may be relative or absolute
			File file = new File(url.substring(FILE_SCHEME.length()));
			if (file.isAbsolute()) {
				this.localFile = file;
			} else {
				if (!baseUrl.startsWith(FILE_SCHEME))
					throw new IllegalArgumentException("Relative file URLs cannot be resolved if the base URL is a non-file URL.");
				File baseFile = new File(url.substring(FILE_SCHEME.length()));
				if (baseFile.isDirectory()) {
					this.localFile = new File(baseFile, file.toString());
				} else {
					this.localFile = new File(baseFile.getParentFile(), file.toString());
				}
			}
			this.url = localFile.toURI().toURL();
		} else if (url.startsWith(HTTP_SCHEME)) {
			// HTTP URLs must be absolute
			this.url = new URL(url);
			this.localFile = null;
		} else {
			// A path with no scheme means resolve relative to the base URL
			URL base = new URL(baseUrl);
			this.url = new URL(base, url);
			this.localFile = null;
		}
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
		
		if (!cachedFile.exists()) {
			File cacheDir = cachedFile.getParentFile();
			if (!cacheDir.mkdirs())
				throw new IOException(String.format("Failed to create cache directory '%s'.", cacheDir.getAbsolutePath()));
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
