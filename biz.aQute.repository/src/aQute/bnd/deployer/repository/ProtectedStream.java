package aQute.bnd.deployer.repository;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is used to prevent content providers with interfering with the state of
 * the underlying stream by calling ant of {@link InputStream#close()},
 * {@link InputStream#mark(int)} or {@link InputStream#reset()}.
 *
 * @author Neil Bartlett
 */
class ProtectedStream extends InputStream {

	private InputStream delegate;

	ProtectedStream(InputStream delegate) {
		this.delegate = delegate;
	}

	@Override
	public int available() throws IOException {
		return delegate.available();
	}

	@Override
	public void close() throws IOException {
		// ignore!
	}

	@Override
	public void mark(int limit) {
		throw new UnsupportedOperationException("mark is not supported");
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		return delegate.read();
	}

	@Override
	public int read(byte[] buf) throws IOException {
		return delegate.read(buf);
	}

	@Override
	public int read(byte[] buf, int start, int len) throws IOException {
		return delegate.read(buf, start, len);
	}

	@Override
	public void reset() throws IOException {
		throw new IOException("Reset not allowed");
	}

	@Override
	public long skip(long bytes) throws IOException {
		return delegate.skip(bytes);
	}

}
