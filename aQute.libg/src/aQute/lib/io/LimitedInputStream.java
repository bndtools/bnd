package aQute.lib.io;

import static java.util.Objects.requireNonNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
	private final InputStream	in;
	private long				remaining;
	private long				marked;

	public LimitedInputStream(InputStream in, int size) {
		this.in = requireNonNull(in);
		this.remaining = size;
		this.marked = size;
		if (size < 0) {
			throw new IllegalArgumentException("size must be non-negative");
		}
	}

	private void consume(long n) throws IOException {
		if (n - remaining > 0L) {
			throw new EOFException("request to read more bytes than available");
		}
		remaining -= n;
	}

	private boolean hasRemaining() {
		return remaining > 0L;
	}

	private long ranged(long n) {
		if (n <= 0L) {
			return 0L;
		}
		return Math.min(n, remaining);
	}

	@Override
	public int read() throws IOException {
		if (hasRemaining()) {
			int read = in.read();
			consume(Byte.BYTES);
			return read;
		}
		eof();
		return -1;
	}

	@Override
	public int available() throws IOException {
		return (int) ranged(in.available());
	}

	@Override
	public void close() throws IOException {
		remaining = 0L;
		eof();
		in.close();
	}

	protected void eof() {}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark((int) ranged(readlimit));
		marked = remaining;
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (hasRemaining()) {
			int read = in.read(b, off, (int) ranged(len));
			consume(read);
			return read;
		}
		eof();
		return -1;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		if (!in.markSupported()) {
			throw new IOException("mark/reset not supported");
		}
		in.reset();
		remaining = marked;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = in.skip(ranged(n));
		consume(skipped);
		return skipped;
	}
}
