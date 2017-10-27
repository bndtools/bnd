package aQute.bnd.osgi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;

import aQute.bnd.osgi.URLResource.JarURLUtil;

public interface Resource extends Closeable {
	InputStream openInputStream() throws Exception;

	void write(OutputStream out) throws Exception;

	long lastModified();

	void setExtra(String extra);

	String getExtra();

	long size() throws Exception;

	ByteBuffer buffer() throws Exception;

	static Resource fromURL(URL url) throws IOException {
		if (url.getProtocol().equalsIgnoreCase("file")) {
			File file = new File(URI.create(url.toExternalForm()));
			return new FileResource(file);
		}
		if (url.getProtocol().equals("jar")) {
			JarURLUtil util = new JarURLUtil(url);
			URL jarFileURL = util.getJarFileURL();
			if (jarFileURL.getProtocol().equalsIgnoreCase("file")) {
				File file = new File(URI.create(jarFileURL.toExternalForm()));
				String entryName = util.getEntryName();
				if (entryName == null) {
					return new FileResource(file);
				}
				return new ZipResource(file, entryName);
			}
		}
		return new URLResource(url);
	}
}
