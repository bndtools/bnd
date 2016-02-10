package aQute.bnd.http;

import java.io.IOException;
import java.io.InputStream;

import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.progress.ProgressPlugin.Task;

public class ProgressWrappingStream extends InputStream {

	private InputStream	delegate;
	private Task		task;

	public ProgressWrappingStream(InputStream delegate, String name, int size, ProgressPlugin progressPlugin) {
		this.delegate = delegate;
		task = progressPlugin.startTask(name, size);
	}

	@Override
	public int read() throws IOException {
		int data = delegate.read();
		update(data == -1 ? -1 : 1);
		return data;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		int count = delegate.read(buffer);
		return update(count);
	}

	public int update(int count) {
		if (count == -1)
			task.done("Completed", null);
		else
			task.worked(count);
		return count;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int count = delegate.read(buffer, offset, length);
		update(count);
		return count;
	}

}
