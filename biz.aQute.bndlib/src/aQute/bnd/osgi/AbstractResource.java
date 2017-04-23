package aQute.bnd.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import aQute.lib.io.IO;

public abstract class AbstractResource implements Resource {
	private String		extra;
	private ByteBuffer	buffer;
	private final long	lastModified;

	protected AbstractResource(long modified) {
		lastModified = modified;
	}

	public String getExtra() {
		return extra;
	}

	public long lastModified() {
		return lastModified;
	}

	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	private ByteBuffer getBuffer() throws Exception {
		if (buffer != null) {
			return buffer;
		}
		return buffer = ByteBuffer.wrap(getBytes());
	}

	@Override
	public ByteBuffer buffer() throws Exception {
		return getBuffer().duplicate();
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
	}

	abstract protected byte[] getBytes() throws Exception;

	public long size() throws Exception {
		return getBuffer().limit();
	}

	public void close() throws IOException {}
}
