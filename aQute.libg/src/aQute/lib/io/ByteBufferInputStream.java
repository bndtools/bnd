package aQute.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
	private final ByteBuffer bb;

	public ByteBufferInputStream(ByteBuffer buffer) {
		buffer.mark();
		bb = buffer;
	}

	@Override
	public int read() throws IOException {
		if (!bb.hasRemaining()) {
			return -1;
		}
		return 0xFF & bb.get();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int remaining = bb.remaining();
		if (remaining <= 0) {
			return -1;
		}
		int length = Math.min(len, remaining);
		bb.get(b, off, length);
		return length;
	}

	@Override
	public long skip(long n) throws IOException {
		if (n <= 0L) {
			return 0L;
		}
		int skipped = Math.min((int) n, bb.remaining());
		bb.position(bb.position() + skipped);
		return skipped;
	}

	@Override
	public int available() throws IOException {
		return bb.remaining();
	}

	@Override
	public void close() throws IOException {
		bb.position(bb.limit());
	}

	@Override
	public void mark(int readlimit) {
		bb.mark();
	}

	@Override
	public void reset() throws IOException {
		bb.reset();
	}

	@Override
	public boolean markSupported() {
		return true;
	}
}
