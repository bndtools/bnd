package aQute.bnd.build;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
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

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.watcher.FileWatcher;
import aQute.lib.watcher.FileWatcher.Builder;

/**
 * Watches a bnd Project for changes and triggers a build automatically.
 */
public class BuildWatcher implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(BuildWatcher.class);

	private Collection<Project>				projects;
	private final Map<File, Project>		fileToProject		= new HashMap<>();
	private final Executor executor;
	private final ScheduledExecutorService scheduledExecutor;
	private final Semaphore semaphore = new Semaphore(1);
	private final AtomicBoolean propertiesChanged = new AtomicBoolean(false);
	private volatile FileWatcher fw;
	private final Consumer<Project>			perProject;

	public BuildWatcher(Collection<Project> projects, Consumer<Project> perProject, Executor executor,
		ScheduledExecutorService scheduledExecutor) throws Exception {
		this.projects = projects;
		this.perProject = perProject;
		this.executor = executor;
		this.scheduledExecutor = scheduledExecutor;
		logger.info("[BuildWatcher] Initialized for projects: {}", projects);
		watch();
	}

	private void watch() throws Exception {
		Builder builder = new FileWatcher.Builder()
			.executor(executor)
			.changed(this::onFileChanged);

		for (Project project : projects) {
			// Watch bnd file
			File bndFile = project.getPropertiesFile();
			builder.file(bndFile);
			fileToProject.put(bndFile, project);

			// Watch included files
			for (File inc : project.getIncluded()) {
				builder.file(inc);
				fileToProject.put(inc, project);
			}

			List<File> watchFolders = Arrays.asList(project.getBase());
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

		logger.info("[BuildWatcher] Watching projects: {}", projects);
		System.out.println(String.format("[BuildWatcher] Watching projects: %s", projects));
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
		logger.info("[BuildWatcher] Detected change to {} ({})", file.getName(), kind);
		System.out.println(String.format("[BuildWatcher] Detected change to %s (%s)", file.getName(), kind));
		Project project = fileToProject.get(file);

		propertiesChanged.compareAndSet(false,
			project.getPropertiesFile().equals(file) ||
			project.getIncluded().contains(file));

		if (semaphore.tryAcquire()) {
			scheduledExecutor.schedule(() -> {
				try {
					logger.info("[BuildWatcher] Rebuilding project: {}", project.getName());
					perProject.accept(project);
					logger.info("[BuildWatcher] Build successful for {}", project.getName());
				} catch (Exception e) {
					logger.error("[BuildWatcher] Build failed for {}", project.getName(), e);
				} finally {
					semaphore.release();

					if (propertiesChanged.compareAndSet(true, false)) {
						logger.info("[BuildWatcher] Detected bnd file change — resetting file watcher.");
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
