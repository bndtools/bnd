package aQute.lib.watcher;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class FileWatcher implements AutoCloseable {
	private final Collection<FileSystemWatcher>	watchers;
	private final CountDownLatch				join;

	FileWatcher(Collection<FileSystemWatcher> watchers, CountDownLatch join) {
		this.watchers = watchers;
		this.join = join;
	}

	@Override
	public void close() {
		watchers.forEach(FileSystemWatcher::close);
	}

	public void await() throws InterruptedException {
		join.await();
	}

	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return join.await(timeout, unit);
	}

	public long getCount() {
		return join.getCount();
	}

	public static class Builder {
		private final List<File>			watching;
		private Executor					executor;
		private BiConsumer<File, String>	changed;

		public Builder() {
			watching = new ArrayList<File>();
		}

		public Builder executor(Executor executor) {
			this.executor = requireNonNull(executor);
			return this;
		}

		public Builder changed(BiConsumer<File, String> changed) {
			this.changed = requireNonNull(changed);
			return this;
		}

		public Builder file(File file) {
			if (file != null) {
				watching.add(file);
			}
			return this;
		}

		public Builder files(Collection<File> files) {
			if (files != null) {
				for (File file : files) {
					file(file);
				}
			}
			return this;
		}

		public Builder files(File... files) {
			if (files != null) {
				for (File file : files) {
					file(file);
				}
			}
			return this;
		}

		public FileWatcher build() throws IOException {
			requireNonNull(executor, "no executor was set");
			requireNonNull(changed, "no changed callback was set");

			Map<FileSystem, FileSystemWatcher> map = new HashMap<>();
			for (File file : watching) {
				Path path = file.toPath();
				FileSystem fs = path.getFileSystem();
				FileSystemWatcher watcher = map.get(fs);
				if (watcher == null) {
					map.put(fs, watcher = new FileSystemWatcher(fs));
				}
				watcher.addPath(path);
			}

			Collection<FileSystemWatcher> watchers = map.values();
			CountDownLatch join = new CountDownLatch(watchers.size());
			for (FileSystemWatcher watcher : watchers) {
				executor.execute(() -> {
					try {
						watcher.watch((path, kind) -> changed.accept(path.toFile(), kind));
					} catch (Exception e) {
						// ignore
					} finally {
						watcher.close();
						join.countDown();
					}
				});
			}

			return new FileWatcher(watchers, join);
		}
	}

	static class FileSystemWatcher {
		private final WatchService	watchService;
		private final Set<Path>		watching;

		FileSystemWatcher(FileSystem fs) throws IOException {
			watchService = fs.newWatchService();
			watching = new HashSet<Path>();
		}

		void addPath(Path path) throws IOException {
			if (watching.add(path)) {
				path.getParent()
					.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
						StandardWatchEventKinds.ENTRY_DELETE);
			}
		}

		void watch(BiConsumer<Path, String> changed) throws Exception {
			for (WatchKey key; (key = watchService.take()) != null;) {
				Path dir = (Path) key.watchable();
				for (WatchEvent<?> event : key.pollEvents()) {
					Path path = dir.resolve((Path) event.context());
					if (watching.contains(path)) {
						changed.accept(path, event.kind()
							.name());
					}
				}
				if (!key.reset()) {
					break;
				}
			}
		}

		void close() {
			try {
				watchService.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
}
