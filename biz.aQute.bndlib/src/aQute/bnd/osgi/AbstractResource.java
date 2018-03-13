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

	@Override
	public String getExtra() {
		return extra;
	}

	@Override
	public long lastModified() {
		return lastModified;
	}

	@Override
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

	@Override
	public void setExtra(String extra) {
		this.extra = extra;
	}

	@Override
	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
	}

	abstract protected byte[] getBytes() throws Exception;

	@Override
	public long size() throws Exception {
		return getBuffer().limit();
	}

	@Override
	public void close() throws IOException {}
}
