package aQute.bnd.build;

import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newDirectoryStream;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private final Workspace				workspace;
	private final Map<String, Project>	models;
	private boolean						changed;

	ProjectTracker(Workspace workspace) {
		changed = true;
		this.workspace = workspace;
		models = new HashMap<>();
	}

	@Override
	public synchronized void close() {
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
		if (!changed) {
			return;
		}
		changed = false;

		Path base = workspace.getBase()
			.toPath();
		Set<String> older = new HashSet<>(models.keySet());
		try (DirectoryStream<Path> directories = newDirectoryStream(base, Files::isDirectory)) {
			for (Path directory : directories) {
				models.compute(directory.getFileName()
					.toString(), (name, project) -> {
						if (project != null) {
							older.remove(name);
							if (directory.equals(project.getBase()
								.toPath()) && project.isValid()) {
								return project;
							}
							IO.close(project);
						}
						if (isRegularFile(directory.resolve(Project.BNDPATH))) {
							project = new Project(workspace, directory.toFile());
							if (project.isValid()) {
								return project;
							}
							IO.close(project);
						}
						return null;
					});
			}
		} catch (IOException e) {
			throw Exceptions.duck(e);
		}

		older.stream()
			.map(models::remove)
			.forEach(IO::close);
	}
}
