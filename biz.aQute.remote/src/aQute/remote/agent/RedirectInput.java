package aQute.remote.agent;

import java.io.*;

public class RedirectInput extends InputStream {

	private InputStream	org;
	private byte[]		ring	= new byte[65536];
	private int			in, out;

	public RedirectInput(InputStream in) throws IOException {
		this.org = in;
	}

	public RedirectInput() {}

	public InputStream getOrg() {
		return org;
	}

	public synchronized void add(String s) throws IOException {
		byte[] bytes = s.getBytes();
		for (int i = 0; i < bytes.length; i++) {
			write(bytes[i]);
		}
	}

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

	public void close() {
		// ignore
	}

	@Override
	public int read() throws IOException {
		synchronized (ring) {
			while (in == out) {
				try {
					ring.wait(400);
				}
				catch (InterruptedException e) {
					return -1;
				}
			}
			int c = 0xFF & ring[out];
			out = (out + 1) % ring.length;
			return c;
		}
	}

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
}
