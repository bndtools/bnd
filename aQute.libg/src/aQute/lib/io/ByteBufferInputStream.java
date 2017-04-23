package aQute.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.util.Objects;

public class ByteBufferInputStream extends InputStream {
	private final ByteBuffer bb;

	public ByteBufferInputStream(ByteBuffer buffer) {
		bb = Objects.requireNonNull(buffer);
	}

	@Override
	public int read() throws IOException {
		if (!bb.hasRemaining()) {
			return -1;
		}
		return bb.get();
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
	public synchronized void mark(int readlimit) {
		if (readlimit < 0) {
			throw new IllegalArgumentException("negative read limit");
		}
		bb.mark();
	}

	@Override
	public synchronized void reset() throws IOException {
		try {
			bb.reset();
		} catch (InvalidMarkException e) {
			bb.rewind();
		}
	}

	@Override
	public boolean markSupported() {
		return true;
	}
}
