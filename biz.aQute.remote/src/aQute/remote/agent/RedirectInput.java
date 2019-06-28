package aQute.remote.agent;

import java.io.IOException;
import java.io.InputStream;

/**
 * An filter stream that takes a string from the supervisor and then provides it
 * as a read to the user of this Input Stream. It also dispatches the text to an
 * original stream.It does not use Piped*Stream because they turned hard to
 * break. This implementation uses a ring buffer.
 */
public class RedirectInput extends InputStream {

	private InputStream	org;
	private byte[]		ring	= new byte[65536];
	private int			in, out;

	/**
	 * Create a redirector input stream with an original input stream
	 *
	 * @param in the original
	 */
	public RedirectInput(InputStream in) throws IOException {
		this.org = in;
	}

	/**
	 * Create a redirector without an original
	 */
	public RedirectInput() {}

	/**
	 * Get the original inputstream, potentially null
	 *
	 * @return null or the original input stream
	 */
	public InputStream getOrg() {
		return org;
	}

	/**
	 * Provide the string that should be treated as input for the running code.
	 *
	 * @param s the string
	 */
	public synchronized void add(String s) throws IOException {
		byte[] bytes = s.getBytes();
		for (int i = 0; i < bytes.length; i++) {
			write(bytes[i]);
		}
	}

	/**
	 * Write to the ring buffer
	 */
	private void write(byte b) {
		synchronized (ring) {
			ring[in] = b;

			in = (in + 1) % ring.length;
			if (in == out) {
				// skip oldest output
				out = (out + 1) % ring.length;
			}
			ring.notifyAll();
		}
	}

	@Override
	public void close() {
		// ignore
	}

	/**
	 * Read a byte from the input buffer. We will be fully interruptible, in the
	 * case of an interrupt we return -1 (eof)
	 */
	@Override
	public int read() throws IOException {
		System.out.flush();
		synchronized (ring) {
			while (in == out) {
				try {
					ring.wait(400);
				} catch (InterruptedException e) {
					return -1;
				}
			}
			int c = 0xFF & ring[out];
			out = (out + 1) % ring.length;
			return c;
		}
	}

	/**
	 * And the read for a larger buffer
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int n = 0;
		for (int i = offset; i < length; i++) {
			int c = read();
			if (c < 0)
				break;
			buffer[i] = (byte) (0xFF & c);
			n++;

			if (c == '\n')
				break;
		}
		return n;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}
}
