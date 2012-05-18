package aQute.lib.deployer.repository;

import java.io.IOException;
import java.io.InputStream;

public class WrappingStream extends InputStream {
	
	private InputStream delegate;

	public WrappingStream(InputStream delegate) {
		this.delegate = delegate;
	}

	public int available() throws IOException {
		return delegate.available();
	}

	public boolean equals(Object var0) {
		return delegate.equals(var0);
	}

	public void close() throws IOException {
		delegate.close();
	}

	public void mark(int var0) {
		delegate.mark(var0);
	}

	public boolean markSupported() {
		return delegate.markSupported();
	}

	public int read() throws IOException {
		return delegate.read();
	}

	public int read(byte[] var0) throws IOException {
		return delegate.read(var0);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public int read(byte[] var0, int var1, int var2) throws IOException {
		return delegate.read(var0, var1, var2);
	}

	public String toString() {
		return delegate.toString();
	}

	public void reset() throws IOException {
		delegate.reset();
	}

	public long skip(long var0) throws IOException {
		return delegate.skip(var0);
	}
	
	

}
