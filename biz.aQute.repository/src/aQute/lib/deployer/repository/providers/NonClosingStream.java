package aQute.lib.deployer.repository.providers;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is used to prevent the XMLStreamReader from closing the stream when it
 * reaches the end of the document -- since when that happens we want to rewind
 * to the start.
 * 
 * @author njbartlett
 * 
 */
public class NonClosingStream extends InputStream {
	
	private InputStream delegate;

	public NonClosingStream(InputStream delegate) {
		this.delegate = delegate;
	}
	
	InputStream getWrappedStream() {
		return delegate;
	}

	public int available() throws IOException {
		return delegate.available();
	}

	public boolean equals(Object var0) {
		return delegate.equals(var0);
	}

	public void close() throws IOException {
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
