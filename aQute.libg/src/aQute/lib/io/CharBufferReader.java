package aQute.lib.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.InvalidMarkException;
import java.util.Objects;

public class CharBufferReader extends Reader {
	private final CharBuffer cb;

	public CharBufferReader(CharBuffer buffer) {
		cb = Objects.requireNonNull(buffer);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int remaining = cb.remaining();
		if (remaining <= 0) {
			return -1;
		}
		int length = Math.min(len, remaining);
		cb.get(cbuf, off, length);
		return length;
	}

	@Override
	public void close() throws IOException {
		cb.position(cb.limit());
	}

	@Override
	public int read() throws IOException {
		if (!cb.hasRemaining()) {
			return -1;
		}
		return cb.get();
	}

	@Override
	public long skip(long n) throws IOException {
		if (n < 0L) {
			throw new IllegalArgumentException("negative skip count");
		}
		int skipped = Math.min((int) n, cb.remaining());
		cb.position(cb.position() + skipped);
		return skipped;
	}

	@Override
	public boolean ready() throws IOException {
		return true;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void mark(int readAheadLimit) throws IOException {
		if (readAheadLimit < 0) {
			throw new IllegalArgumentException("negative read ahead limit");
		}
		cb.mark();
	}

	@Override
	public void reset() throws IOException {
		try {
			cb.reset();
		} catch (InvalidMarkException e) {
			cb.rewind();
		}
	}
}
