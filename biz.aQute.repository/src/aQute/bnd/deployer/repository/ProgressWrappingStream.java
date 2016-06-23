package aQute.bnd.deployer.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.progress.ProgressPlugin.Task;

public class ProgressWrappingStream extends InputStream {

	private InputStream	delegate;
	private List<Task>	tasks;

	public ProgressWrappingStream(InputStream delegate, String name, int size, List<ProgressPlugin> progressPlugins) {
		this.delegate = delegate;
		this.tasks = new ArrayList<>();
		for (ProgressPlugin progressPlugin : progressPlugins) {
			tasks.add(progressPlugin.startTask(name, size));
		}
	}

	@Override
	public int read() throws IOException {
		int data = delegate.read();
		if (data == -1) {
			for (Task task : tasks) {
				task.done("Completed", null);
			}
		} else {
			for (Task task : tasks) {
				task.worked(1);
			}
		}
		return data;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		int count = delegate.read(buffer);
		if (count == -1) {
			for (Task task : tasks) {
				task.done("Completed", null);
			}
		} else {
			for (Task task : tasks) {
				task.worked(count);
			}
		}
		return count;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int count = delegate.read(buffer, offset, length);
		if (count == -1) {
			for (Task task : tasks) {
				task.done("Completed", null);
			}
		} else {
			for (Task task : tasks) {
				task.worked(count);
			}
		}
		return count;
	}

}
