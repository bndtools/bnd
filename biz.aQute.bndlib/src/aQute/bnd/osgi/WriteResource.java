package aQute.bnd.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;

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
		ByteBufferOutputStream out = new ByteBufferOutputStream();
		write(out);
		return buffer = out.toByteBuffer();
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
