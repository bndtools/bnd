package aQute.bnd.deployer.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.progress.ProgressPlugin.Task;

public class ProgressWrappingStream extends InputStream {

	private InputStream	delegate;
	private Task		task;

	public ProgressWrappingStream(InputStream delegate, String name, int size, ProgressPlugin progressPlugin) {
		this(delegate, name, size, safeList(progressPlugin));
	}

	public ProgressWrappingStream(InputStream delegate, String name, int size, List<ProgressPlugin> progressPlugins) {
		this.delegate = delegate;

		if (progressPlugins != null && progressPlugins.size() > 1) {
			final List<ProgressPlugin.Task> multiplexedTasks = new ArrayList<>();

			for (ProgressPlugin progressPlugin : progressPlugins) {
				multiplexedTasks.add(progressPlugin.startTask(name, size));
			}

			task = new ProgressPlugin.Task() {
				@Override
				public void worked(int units) {
					for (ProgressPlugin.Task task : multiplexedTasks) {
						task.worked(units);
					}
				}

				@Override
				public void done(String message, Throwable e) {
					for (ProgressPlugin.Task task : multiplexedTasks) {
						task.done(message, e);
					}
				}

				@Override
				public boolean isCanceled() {
					for (ProgressPlugin.Task task : multiplexedTasks) {
						if (task.isCanceled()) {
							return true;
						}
					}
					return false;
				}
			};
		} else if (progressPlugins != null && progressPlugins.size() == 1) {
			task = progressPlugins.get(0)
				.startTask(name, size);
		} else {
			task = new ProgressPlugin.Task() {
				@Override
				public void worked(int units) {}

				@Override
				public void done(String message, Throwable e) {}

				@Override
				public boolean isCanceled() {
					return Thread.currentThread()
						.isInterrupted();
				}
			};
		}
	}

	private static <T> List<T> safeList(T o) {
		if (o == null) {
			return Collections.emptyList();
		} else {
			return Collections.singletonList(o);
		}
	}

	@Override
	public int read() throws IOException {
		int data = delegate.read();
		if (data == -1)
			task.done("Completed", null);
		else
			task.worked(1);
		return data;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		int count = delegate.read(buffer);
		if (count == -1)
			task.done("Completed", null);
		else
			task.worked(count);
		return count;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int count = delegate.read(buffer, offset, length);
		if (count == -1)
			task.done("Completed", null);
		else
			task.worked(count);
		return count;
	}

}
