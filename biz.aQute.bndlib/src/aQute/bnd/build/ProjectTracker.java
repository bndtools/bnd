package aQute.bnd.build;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

/**
 * This class is responsible for maintaining the project list. Since this list
 * can change asynchronously we guard access from here.
 */
class ProjectTracker implements AutoCloseable {
	final Map<String, Project>	models	= new HashMap<>();
	final Workspace				workspace;
	final Path					base;
	final WatchService			watchService;
	final WatchKey				watchKey;

	boolean						changed	= true;

	ProjectTracker(Workspace workspace) throws IOException {
		this.workspace = workspace;
		base = workspace.getBase()
			.toPath();
		watchService = base.getFileSystem()
			.newWatchService();
		watchKey = base.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
	}

	@Override
	public synchronized void close() {
		IO.close(watchService);
		models.values()
			.forEach(IO::close);
	}

	/*
	 * Next time the list must be refreshed
	 */
	synchronized void refresh() {
		changed = true;
	}

	/*
	 * Answer a snapshot of the current list of projects
	 */
	synchronized Set<Project> getAllProjects() {
		update();
		return new HashSet<>(models.values());
	}

	/*
	 * Answer the project with the given name
	 */
	synchronized Optional<Project> getProject(String name) {
		update();
		return Optional.ofNullable(models.get(name));
	}

	/*
	 * Sync the directories with the current lists of projects
	 */
	private void update() {
		try {
			for (WatchKey key = watchService.poll(); key != null; key = watchService.poll()) {
				changed = true;
				key.pollEvents();
				key.reset();
			}
		} catch (ClosedWatchServiceException e) {}

		if (!changed) {
			return;
		}

		Set<String> older = new HashSet<>(models.keySet());

		try (DirectoryStream<Path> directories = Files.newDirectoryStream(base, Files::isDirectory)) {
			for (Path directory : directories) {
				String name = directory.getFileName()
					.toString();
				if (models.containsKey(name)) {
					older.remove(name);
					continue;
				}
				if (Files.isRegularFile(directory.resolve(Project.BNDPATH))) {
					Project project = new Project(workspace, directory.toFile());
					if (project.isValid()) {
						models.put(project.getName(), project);
					} else {
						IO.close(project);
					}
				}
			}
		} catch (IOException e) {
			throw Exceptions.duck(e);
		}

		older.forEach(name -> IO.close(models.remove(name)));

		changed = false;
	}
}
