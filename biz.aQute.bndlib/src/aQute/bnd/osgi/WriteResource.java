package aQute.bnd.osgi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;

public abstract class WriteResource implements Resource {
	private ByteBuffer	buffer;
	private String		extra;

	public ByteBuffer buffer() throws Exception {
		return getBuffer().duplicate();
	}

	private ByteBuffer getBuffer() throws Exception {
		if (buffer != null) {
			return buffer;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(IOConstants.PAGE_SIZE);
		write(out);
		return buffer = ByteBuffer.wrap(out.toByteArray());
	}

	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	public abstract void write(OutputStream out) throws Exception;

	public abstract long lastModified();

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public long size() throws Exception {
		return getBuffer().limit();
	}

	public void close() throws IOException {}
}
