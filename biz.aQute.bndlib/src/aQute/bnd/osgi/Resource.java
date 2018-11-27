package aQute.bnd.osgi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Locale;

import aQute.bnd.http.HttpClient;
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
		return fromURL(url, null);
	}

	static Resource fromURL(URL url, HttpClient client) throws IOException {
		String protocol = url.getProtocol()
			.toLowerCase(Locale.ROOT);
		if (protocol.equals("file")) {
			URI uri = URI.create(url.toExternalForm());
			Path path = new File(uri.getSchemeSpecificPart()).toPath()
				.toAbsolutePath();
			return new FileResource(path);
		}
		if (protocol.equals("jar")) {
			JarURLUtil util = new JarURLUtil(url);
			URL jarFileURL = util.getJarFileURL();
			if (jarFileURL.getProtocol()
				.equalsIgnoreCase("file")) {
				URI uri = URI.create(jarFileURL.toExternalForm());
				Path path = new File(uri.getSchemeSpecificPart()).toPath()
					.toAbsolutePath();
				String entryName = util.getEntryName();
				if (entryName == null) {
					return new FileResource(path);
				}
				return new ZipResource(path, entryName);
			}
		}
		return new URLResource(url, protocol.equals("jrt") ? null : client);
	}
}
