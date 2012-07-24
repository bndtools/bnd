package aQute.bnd.osgi;

import java.io.*;

public abstract class WriteResource implements Resource {
	String			extra;
	volatile long	size	= -1;

	public InputStream openInputStream() throws Exception {
		PipedInputStream pin = new PipedInputStream();
		final PipedOutputStream pout = new PipedOutputStream(pin);
		Thread t = new Thread() {
			public void run() {
				try {
					write(pout);
					pout.flush();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					try {
						pout.close();
					}
					catch (IOException e) {
						// Ignore
					}
				}
			}
		};
		t.start();
		return pin;
	}

	public abstract void write(OutputStream out) throws IOException, Exception;

	public abstract long lastModified();

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	static class CountingOutputStream extends OutputStream {
		long	size;

		@Override
		public void write(int var0) throws IOException {
			size++;
		}

		@Override
		public void write(byte[] buffer) throws IOException {
			size += buffer.length;
		}

		@Override
		public void write(byte[] buffer, int start, int length) throws IOException {
			size += length;
		}
	}

	public long size() throws IOException, Exception {
		if (size == -1) {
			CountingOutputStream cout = new CountingOutputStream();
			write(cout);
			size = cout.size;
		}
		return size;
	}
}
