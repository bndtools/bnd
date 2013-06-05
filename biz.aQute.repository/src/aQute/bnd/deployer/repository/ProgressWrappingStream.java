package aQute.bnd.deployer.repository;

import java.io.*;

import aQute.bnd.service.*;

public class ProgressWrappingStream extends InputStream {
	
	private InputStream	delegate;
	private ProgressPlugin	progress;

	public ProgressWrappingStream(InputStream delegate, String name, int size, ProgressPlugin progress) {
		this.delegate = delegate;
		this.progress = progress;
		
		progress.startedTask(name, size);
	}

	@Override
	public int read() throws IOException {
		int data = delegate.read();
		progress.worked(1);
		return data;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		int count = delegate.read(buffer);
		progress.worked(count);
		return count;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int count = delegate.read(buffer, offset, length);
		progress.worked(count);
		return count;
	}

}
