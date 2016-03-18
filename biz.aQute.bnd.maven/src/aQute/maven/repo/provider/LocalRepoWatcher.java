package aQute.maven.repo.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import aQute.lib.collections.MultiMap;
import aQute.maven.repo.api.Program;
import aQute.maven.repo.api.Revision;
import aQute.service.reporter.Reporter;

public class LocalRepoWatcher implements Closeable {

	public interface RevisionChanged {
		public boolean revisionChanged(Revision revision) throws Exception;
	}

	private static final int			COALESCE_TIME	= 2000;
	private final Executor				executor;
	private final File					root;
	private final WatchService			watchService;
	private final Callable<Boolean>		callback;
	private final Reporter				reporter;
	private boolean						quit			= false;
	private MultiMap<Program,Revision>	revisions		= new MultiMap<>();
	private List<Revision>				changed			= new ArrayList<>();

	public LocalRepoWatcher(Executor executor, File root, Reporter reporter, Callable<Boolean> callback)
			throws Exception {
		this.executor = executor == null ? Executors.newCachedThreadPool() : executor;
		this.root = root;
		this.reporter = reporter;
		this.callback = callback;
		watchService = root.toPath().getFileSystem().newWatchService();
		Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (isProjectDir(dir))
					return FileVisitResult.SKIP_SUBTREE;

				WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
				return FileVisitResult.CONTINUE;
			}

			boolean isProjectDir(Path dir) {
				return dir.toFile().isFile() || dir.resolve("pom.xml").toFile().isFile();
			}
		});
	}

	public void open() {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					watch();
				} catch (Exception e) {
					reporter.exception(e, "Running watcher on directory %s", root);
				}
			}
		});
	}

	void watch() throws Exception {
		long notify = Long.MAX_VALUE;

		while (!quit) {
			WatchKey key = watchService.poll(500, TimeUnit.SECONDS);

			if (key != null) {
				List<WatchEvent<Path>> pollEvents = coerce(key.pollEvents());
				for (WatchEvent<Path> we : pollEvents) {
					Path p = root.toPath().resolve(we.context());
					File f = p.toFile();
					if (we.count() == 1) {
						if (f.isDirectory()) {
							if (we.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
								p.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
										StandardWatchEventKinds.ENTRY_DELETE);
							}
						} else if (f.isFile()) {
							if (f.getName().equals("pom.xml")) {
								Path programPath = we.context().getParent().getParent();
								StringBuilder sb = new StringBuilder();
								String del = "";
								int l = programPath.getNameCount();
								for (int i = 0; i < l - 1; i++) {
									sb.append(del);
									sb.append(programPath.getName(i));
									del = ".";
								}
								String group = sb.toString();
								String artifact = programPath.getName(l - 1).toString();
								String version = we.context().getParent().getFileName().toString();
								Program program = Program.valueOf(group, artifact);
								Revision r = program.version(version);
								synchronized (revisions) {
									if (we.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
										revisions.add(program, r);
									} else if (we.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
										revisions.removeValue(program, r);
									}
								}
								changed.add(r);
								callback.call();
							}
						}
					}
				}
				notify = System.currentTimeMillis() + COALESCE_TIME;
			} else {
				if (notify < System.currentTimeMillis()) {
					notify = Long.MAX_VALUE;
					if (!callback.call()) {
						close();
						return;
					}
				}
			}
		}
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private List<WatchEvent<Path>> coerce(List<WatchEvent< ? >> pollEvents) {
		List x = pollEvents;
		return x;
	}

	@Override
	public void close() throws IOException {
		this.quit = true;
		watchService.close();
	}

	public List<Program> getLocalPrograms() {
		synchronized (revisions) {
			return new ArrayList<Program>(revisions.keySet());
		}
	}

}
