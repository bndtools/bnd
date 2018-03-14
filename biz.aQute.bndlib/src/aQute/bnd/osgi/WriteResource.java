package aQute.bnd.osgi;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;

public abstract class WriteResource implements Resource {
	private ByteBuffer	buffer;
	private String		extra;

	@Override
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

	@Override
	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	@Override
	public abstract void write(OutputStream out) throws Exception;

	@Override
	public abstract long lastModified();

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
		return getBuffer().limit();
	}

	@Override
	public void close() {}
}
