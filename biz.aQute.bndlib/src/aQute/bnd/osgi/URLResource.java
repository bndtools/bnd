package aQute.bnd.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.jar.JarFile;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;

class URLResource implements Resource {
	private static final ByteBuffer	CLOSED			= ByteBuffer.allocate(0);
	private ByteBuffer				buffer;
	private final URL				url;
	private final HttpClient		client;
	private String					extra;
	private long					lastModified	= -1L;
	private int						size			= -1;

	/**
	 * This constructor is not for use other than by
	 * {@link Resource#fromURL(URL)}.
	 *
	 * @see Resource#fromURL(URL)
	 */
	URLResource(URL url, HttpClient client) {
		this.url = url;
		this.client = client;
	}

	@Override
	public ByteBuffer buffer() throws Exception {
		return getBuffer().duplicate();
	}

	private ByteBuffer getBuffer() throws Exception {
		if (buffer != null) {
			return buffer;
		}
		InputStream in = open();
		if (size == -1) {
			return buffer = ByteBuffer.wrap(IO.read(in));
		}
		ByteBuffer bb = IO.copy(in, ByteBuffer.allocate(size));
		bb.flip();
		return buffer = bb;
	}

	private InputStream open() throws Exception {
		URLConnection conn;
		InputStream in;
		if (client != null) {
			TaggedData tag = client.connectTagged(url);
			conn = tag.getConnection();
			in = tag.getInputStream();
		} else {
			conn = url.openConnection();
			conn.connect();
			in = conn.getInputStream();
		}
		lastModified = conn.getLastModified();
		int length = conn.getContentLength();
		if (length != -1) {
			size = length;
		}
		return in;
	}

	@Override
	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	@Override
	public String toString() {
		return ":" + url.toExternalForm() + ":";
	}

	@Override
	public void write(OutputStream out) throws Exception {
		if (buffer != null) {
			IO.copy(buffer(), out);
		} else {
			IO.copy(open(), out);
		}
	}

	@Override
	public long lastModified() {
		if (lastModified >= 0L) {
			return lastModified;
		}
		try {
			getBuffer();
		} catch (Exception e) {
			lastModified = 0L;
		}
		return lastModified;
	}

	@Override
	public String getExtra() {
		return extra;
	}

	@Override
	public void setExtra(String extra) {
		this.extra = extra;
	}

	@Override
	public long size() throws Exception {
		if (size >= 0) {
			return size;
		}
		return size = getBuffer().limit();
	}

	@Override
	public void close() throws IOException {
		/*
		 * Allow original buffer to be garbage collected and prevent it being
		 * remapped for this URLResouce.
		 */
		buffer = CLOSED;
	}

	/**
	 * Use JarURLConnection to parse jar: URL into URL to jar URL and entry.
	 */
	static class JarURLUtil extends JarURLConnection {
		JarURLUtil(URL url) throws MalformedURLException {
			super(url);
		}

		@Override
		public JarFile getJarFile() throws IOException {
			return null;
		}

		@Override
		public void connect() throws IOException {}
	}
}
