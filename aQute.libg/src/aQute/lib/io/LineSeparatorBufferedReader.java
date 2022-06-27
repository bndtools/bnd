package aQute.lib.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * BufferedReader which returns the line separator string for the
 * previously read line.
 */
public class LineSeparatorBufferedReader extends BufferedReader {
	public LineSeparatorBufferedReader(Reader in) {
		super(in);
	}

	public LineSeparatorBufferedReader(Reader in, int size) {
		super(in, size);
	}

	private int markedPushBack = -1;
	private int pushBack = -1;
	private int markedEol;
	private int eol;

	@Override
	public void mark(int readAheadLimit) throws IOException {
		super.mark(readAheadLimit);
		markedPushBack = pushBack;
		markedEol = eol;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		pushBack = markedPushBack;
		eol = markedEol;
	}

	@Override
	public int read() throws IOException {
		lineSeparator(); // consume any line separator characters
		return read0();
	}

	private int read0() throws IOException {
		int c = pushBack;
		if (c != -1) {
			pushBack = -1;
			return c;
		}
		return super.read();
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		lineSeparator(); // consume any line separator characters
		int c = pushBack;
		if (c != -1) {
			if ((off >= 0) && (off < cbuf.length) && (len > 0)) {
				pushBack = -1;
				cbuf[off] = (char) c;
				return 1;
			}
		}
		return super.read(cbuf, off, len);
	}

	@Override
	public int read(CharBuffer target) throws IOException {
		lineSeparator(); // consume any line separator characters
		int c = pushBack;
		if (c != -1) {
			if (target.remaining() > 0) {
				pushBack = -1;
				target.put((char) c);
				return 1;
			}
		}
		return super.read(target);
	}

	@Override
	public int read(char[] cbuf) throws IOException {
		return read(cbuf, 0, cbuf.length);
	}

	@Override
	public String readLine() throws IOException {
		int c = read0();
		if (c == -1) {
			eol = 0;
			return null;
		}
		StringBuilder sb = new StringBuilder(80);
		for (; c != -1; c = read0()) {
			if (c == '\n') {
				if (eol == '\r') {
					eol = 0;
					continue;
				}
				eol = c;
				return sb.toString();
			}
			if (c == '\r') {
				eol = c;
				return sb.toString();
			}
			sb.append((char)c);
		}
		eol = 0;
		return sb.toString();
	}

	@Override
	public long skip(long n) throws IOException {
		lineSeparator(); // consume any line separator characters
		if ((pushBack != -1) && (n > 0L)) {
			pushBack = -1;
			return super.skip(n - 1) + 1;
		}
		return super.skip(n);
	}

	@Override
	public boolean ready() throws IOException {
		return (pushBack != -1) || super.ready();
	}

	/**
	 * Return the line separator string from the previously read line
	 * using {@link #readLine()} or the empty string if end of file.
	 * This method can be called once per read line. Subsequent calls
	 * per read line will return the empty string.
	 *
	 * @return The line separator string from the previously read line.
	 * @throws IOException If an exception occurs reading.
	 */
	public String lineSeparator() throws IOException {
		int e = eol;
		if (e == '\n') {
			eol = 0;
			return "\n";
		}
		if (e == '\r') {
			eol = 0;
			int c = read0();
			if (c != '\n') {
				pushBack = c;
				return "\r";
			}
			return "\r\n";
		}
		return "";
	}

}
