package aQute.bnd.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import aQute.lib.io.IO;

public class URLResource implements Resource {
	private static final ByteBuffer	CLOSED			= ByteBuffer.allocate(0);
	private ByteBuffer		buffer;
	private final URL		url;
	private String			extra;
	private long			lastModified	= -1L;
	private int						size			= -1;

	public URLResource(URL url) {
		this.url = url;
	}

	@Override
	public ByteBuffer buffer() throws Exception {
		return getBuffer().duplicate();
	}

	private ByteBuffer getBuffer() throws Exception {
		if (buffer != null) {
			return buffer;
		}
		if (url.getProtocol().equals("file")) {
			File file = new File(url.getPath());
			lastModified = file.lastModified();
			return buffer = IO.read(file.toPath());
		}
		URLConnection conn = openConnection();
		if (size == -1) {
			return buffer = ByteBuffer.wrap(IO.read(conn.getInputStream()));
		}
		ByteBuffer bb = ByteBuffer.allocate(size);
		IO.copy(conn.getInputStream(), bb);
		bb.flip();
		return buffer = bb;
	}

	private URLConnection openConnection() throws Exception {
		URLConnection conn = url.openConnection();
		conn.connect();
		lastModified = conn.getLastModified();
		int length = conn.getContentLength();
		if (length != -1) {
			size = length;
		}
		return conn;
	}

	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	@Override
	public String toString() {
		return ":" + url.toExternalForm() + ":";
	}

	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
	}

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

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

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
}
