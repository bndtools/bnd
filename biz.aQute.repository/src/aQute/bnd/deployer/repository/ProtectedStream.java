package aQute.bnd.deployer.repository;

import java.io.*;

/**
 * This is used to prevent content providers with interfering with the state of
 * the underlying stream by calling ant of {@link InputStream#close()},
 * {@link InputStream#mark(int)} or {@link InputStream#reset()}.
 * 
 * @author Neil Bartlett
 */
class ProtectedStream extends InputStream {

	private InputStream	delegate;

	ProtectedStream(InputStream delegate) {
		this.delegate = delegate;
	}

	public int available() throws IOException {
		return delegate.available();
	}

	public void close() throws IOException {
		// ignore!
	}

	public void mark(int limit) {
		throw new UnsupportedOperationException("mark is not supported");
	}

	public boolean markSupported() {
		return false;
	}

	public int read() throws IOException {
		return delegate.read();
	}

	public int read(byte[] buf) throws IOException {
		return delegate.read(buf);
	}

	public int read(byte[] buf, int start, int len) throws IOException {
		return delegate.read(buf, start, len);
	}

	public void reset() throws IOException {
		throw new IOException("Reset not allowed");
	}

	public long skip(long bytes) throws IOException {
		return delegate.skip(bytes);
	}

}
