package aQute.lib.io;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
	static final int	BUFFER_SIZE	= IOConstants.PAGE_SIZE * 1;

	final InputStream	in;
	final int			size;
	int					left;

	public LimitedInputStream(InputStream in, int size) {
		this.in = in;
		this.left = size;
		this.size = size;
	}

	@Override
	public int read() throws IOException {
		if (left <= 0) {
			eof();
			return -1;
		}

		left--;
		return in.read();
	}

	@Override
	public int available() throws IOException {
		return Math.min(left, in.available());
	}

	@Override
	public void close() throws IOException {
		eof();
		in.close();
	}

	protected void eof() {}

	@Override
	public void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int min = Math.min(len, left);
		if (min == 0)
			return 0;

		int read = in.read(b, off, min);
		if (read > 0)
			left -= read;
		return read;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long skip(long n) throws IOException {
		long count = 0;
		byte buffer[] = new byte[BUFFER_SIZE];
		while (n > 0 && read() >= 0) {
			int size = read(buffer);
			if (size <= 0)
				return count;
			count += size;
			n -= size;
		}
		return count;
	}
}
