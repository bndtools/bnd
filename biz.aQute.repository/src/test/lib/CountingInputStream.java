package test.lib;

import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends InputStream {
	
	private final InputStream delegate;
	
	private long count = 0;
	
	public CountingInputStream(InputStream delegate) {
		this.delegate = delegate;
	}
	
	public long getCount() {
		return count;
	}

	@Override
	public int read() throws IOException {
		count++;
		System.out.printf(".");
		return delegate.read();
	}

	public int available() throws IOException {
		return 1; //delegate.available();
	}

	public void close() throws IOException {
		delegate.close();
	}
	
	

	/*
	public int read(byte[] bytes) throws IOException {
		int incr = delegate.read(bytes);
		count += incr;
		return incr;
	}

	public int read(byte[] var0, int start, int length) throws IOException {
		int incr = delegate.read(var0, start, length);
		count += incr;
		return incr;
	}
	*/

}
