package aQute.bnd.build;

import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import aQute.lib.io.IO;

/**
 * This class is responsible for maintaining the project list. Since this list
 * can change asynchronously we guard access from here.
 */
class ProjectTracker implements AutoCloseable {
	private final Workspace						workspace;
	private final Map<CollationKey, Project>	projects;
	private boolean								changed;
	private final Collator						fileCollator	= IO.fileCollator();

	ProjectTracker(Workspace workspace) {
		changed = true;
		this.workspace = workspace;
		projects = new TreeMap<>();
	}

	@Override
	public synchronized void close() {
		projects.values()
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
	synchronized List<Project> getAllProjects() {
		update();
		return new ArrayList<>(projects.values());
	}

	/*
	 * Answer the project with the given name
	 */
	synchronized Optional<Project> getProject(String name) {
		update();
		CollationKey key = fileCollator.getCollationKey(name);
		if (!projects.containsKey(key) && workspace.getFile(name + "/" + Project.BNDFILE)
			.isFile()) {
			changed = true;
			update();
		}
		return Optional.ofNullable(projects.get(key));
	}

	private List<CollationKey> list(Path dir) {
		if ((dir != null) && Files.isDirectory(dir)) {
			try (Stream<Path> stream = Files.list(dir)) {
				List<CollationKey> result = stream.filter(Files::isDirectory)
					.map(path -> fileCollator.getCollationKey(path.getFileName()
						.toString()))
					.sorted()
					.collect(toList());
				return result;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return Collections.emptyList();
	}

	/*
	 * Sync the directories with the current lists of projects
	 */
	private void update() {
		if (!changed) {
			return;
		}
		changed = false;

		boolean notify = false;
		try {
			Path base = workspace.getBase()
				.toPath();
			List<CollationKey> older = new ArrayList<>(projects.keySet());
			for (CollationKey key : list(base)) {
				String name = key.getSourceString();
				Path directory = base.resolve(name);
				Project project = projects.get(key);
				if (project != null) {
					older.remove(key);
					if (directory.equals(project.getBase()
						.toPath()) && project.isValid()) {
						continue;
					}
					IO.close(project);
				}
				if (isRegularFile(directory.resolve(Project.BNDPATH))) {
					project = new Project(workspace, directory.toFile());
					if (project.isValid()) {
						projects.put(key, project);
						notify = true;
						continue;
					}
					IO.close(project);
				}
				projects.remove(key);
				notify = true;
			}

			older.stream()
				.map(projects::remove)
				.forEach(IO::close);
		} finally {
			if (notify) {
				workspace.notifier.projects(new ArrayList<>(projects.values()));
			}
		}
	}

	@Override
	public String toString() {
		return projects.keySet()
			.stream()
			.map(CollationKey::getSourceString)
			.collect(joining(",", "[", "]"));
	}

	synchronized void forceRefresh() {
		changed = true;
		update();
	}
}
