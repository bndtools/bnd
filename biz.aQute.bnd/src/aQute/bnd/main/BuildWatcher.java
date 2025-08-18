package aQute.bnd.main;

import java.io.Closeable;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.watcher.FileWatcher;
import aQute.lib.watcher.FileWatcher.Builder;

/**
 * Watches a bnd Project for changes and triggers a build automatically. TODO We
 * could move it to same package as aQute.bnd.build.ProjectLauncher.LiveCoding
 * since it was based on that.
 */
class BuildWatcher implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(BuildWatcher.class);

	private Collection<Project>				projects;
	private final Map<File, Project>		fileToProject		= new HashMap<>();
	private final Executor					watchExecutor;
	private final ScheduledExecutorService	buildScheduler;
	private final Semaphore semaphore = new Semaphore(1);
	private final AtomicBoolean propertiesChanged = new AtomicBoolean(false);
	private volatile FileWatcher fw;
	private final Consumer<Project>			perProject;
	private final PrintStream				out;

	public BuildWatcher(Collection<Project> projects, Consumer<Project> perProject, Executor executor,
		ScheduledExecutorService scheduledExecutor, PrintStream out) throws Exception {
		this.projects = projects;
		this.perProject = perProject;
		this.watchExecutor = executor;
		this.buildScheduler = scheduledExecutor;
		this.out = out;
		watch();
	}

	private void watch() throws Exception {
		Builder builder = new FileWatcher.Builder()
			.executor(watchExecutor)
			.changed(this::onFileChanged);

		for (Project project : projects) {

			List<File> watchFolders = Collections.singletonList(project.getBase());
			Collection<File> watchedFiles = collectSourceFolders(project, watchFolders);
			builder.files(watchedFiles);
			for (File f : watchedFiles) {
				fileToProject.put(f, project);
			}
		}

		FileWatcher old = fw;
		fw = builder.build();
		if (old != null) {
			old.close();
		}
		out.println(String.format("[BuildWatcher] Watching projects (%s files and folders): %s ",
			fileToProject.size(), projects));
	}

	private static Collection<File> collectSourceFolders(Project project, Collection<File> sourceRoots) {
		try {
			Path targetDir = project.getTarget()
				.toPath()
				.normalize()
				.toAbsolutePath();

			Set<File> files = new LinkedHashSet<>();
			for (File root : sourceRoots) {
				if (root.isDirectory()) {
					IO.tree(root)
					.stream()
						// exclude target-dir, .class files etc.
						.filter(f -> !f.getName()
							.endsWith(".class")
							&& !f.toPath()
							.startsWith(targetDir))
					.forEach(files::add);
				}
			}
			return files;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void onFileChanged(File file, String kind) {
		out.println(String.format("[BuildWatcher] Detected change to %s (%s)", file.getName(), kind));
		Project project = fileToProject.get(file);

		propertiesChanged.compareAndSet(false,
			project.getPropertiesFile().equals(file) ||
			project.getIncluded().contains(file));

		if (semaphore.tryAcquire()) {
			buildScheduler.schedule(() -> {
				try {
					logger.debug("[BuildWatcher] Rebuilding project: {}", project.getName());
					perProject.accept(project);
					logger.debug("[BuildWatcher] Build successful for {}", project.getName());
				} catch (Exception e) {
					logger.error("[BuildWatcher] Build failed for {}", project.getName(), e);
				} finally {
					semaphore.release();

					if (propertiesChanged.compareAndSet(true, false)) {
						logger.debug("[BuildWatcher] Detected bnd file change — resetting file watcher.");
						try {
							watch();
						} catch (Exception e) {
							logger.error("[BuildWatcher] Failed to reset watcher", e);
						}
					}
				}
			}, 600, TimeUnit.MILLISECONDS); // debounce delay
		}
	}

	@Override
	public void close() {
		if (fw != null) {
			fw.close();
		}
	}
}
