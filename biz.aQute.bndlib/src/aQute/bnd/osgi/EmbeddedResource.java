package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import aQute.lib.io.IO;

public class EmbeddedResource implements Resource {
	private final ByteBuffer	buffer;
	private final long			lastModified;
	private String				extra;

	public EmbeddedResource(byte data[], long lastModified) {
		this(data, 0, data.length, lastModified);
	}

	public EmbeddedResource(byte data[], int off, int len, long lastModified) {
		this(ByteBuffer.wrap(data, off, len), lastModified);
	}

	public EmbeddedResource(ByteBuffer buffer, long lastModified) {
		this.buffer = buffer;
		this.lastModified = lastModified;
	}

	public EmbeddedResource(String pc, long lastModified) {
		this(pc.getBytes(UTF_8), lastModified);
	}

	@Override
	public ByteBuffer buffer() {
		return buffer.duplicate();
	}

	@Override
	public InputStream openInputStream() {
		return IO.stream(buffer());
	}

	@Override
	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
	}

	@Override
	public String toString() {
		return ":" + size() + ":";
	}

	@Override
	public long lastModified() {
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
	public long size() {
		return buffer.limit();
	}

	@Override
	public void close() throws IOException {}
}
