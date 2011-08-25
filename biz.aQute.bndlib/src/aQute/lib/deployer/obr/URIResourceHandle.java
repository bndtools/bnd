package aQute.lib.deployer.obr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import aQute.bnd.service.ResourceHandle;

public class URIResourceHandle implements ResourceHandle {
	
	static final String FILE_SCHEME = "file";
	
	private final URI uri;
	private final File localFile;
	private final File cachedFile;

	public URIResourceHandle(URI uri, URI baseUri, File cacheDir) throws URISyntaxException {
		if (FILE_SCHEME.equals(uri.getScheme())) {
			String path = uri.getSchemeSpecificPart();
			if (path.length() > 0 && path.charAt(0) != '/')
				uri = new URI(null, null, path, null);
		}
		uri = baseUri.resolve(uri);
		this.uri = uri;
		
		if (FILE_SCHEME.equals(uri.getScheme())) {
			localFile = new File(uri.getPath());
			cachedFile = null;
		} else {
			localFile = null;
			cachedFile = mapRemoteURI(uri, cacheDir);
		}
	}

	public String getName() {
		return uri.toString();
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
			downloadToFile(uri, cachedFile);
		}
		return cachedFile;
	}
	
	private static File mapRemoteURI(URI uri, File cacheDir) {
		String path = uri.getPath();
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		path = path.replace('/', '_');
		return new File(cacheDir, path);
	}

	private static void downloadToFile(URI uri, File file) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = uri.toURL().openStream();
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
