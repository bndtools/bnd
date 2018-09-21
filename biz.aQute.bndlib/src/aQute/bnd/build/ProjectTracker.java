package aQute.bnd.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import aQute.lib.io.IO;

/**
 * This class is responsible for maintaining the project list. Since this list
 * can change asynchronously we guard access from here.
 */
class ProjectTracker {
	final Map<String, Project>	models		= new HashMap<>();
	final List<Project>			buildOrder	= new ArrayList<>();
	final Workspace				workspace;

	boolean						inited		= false;

	ProjectTracker(Workspace workspace) {
		this.workspace = workspace;
	}

	/*
	 * Next time the list must be refreshed
	 */
	synchronized void refresh() {
		inited = false;
	}

	/*
	 * Answer a snapshot of the current list of projects
	 */
	synchronized Set<Project> getAllProjects() {
		init();
		return new HashSet<>(models.values());
	}

	/*
	 * Answer the project with the given name
	 */
	synchronized Optional<Project> getProject(String name) {
		init();
		return Optional.ofNullable(models.get(name));
	}

	/*
	 * Sync the directories with the current lists of projects
	 */
	private void init() {
		if (inited)
			return;

		inited = true;

		Set<String> older = new HashSet<>(models.keySet());
		for (File projectDir : workspace.getBase()
			.listFiles()) {
			if (projectDir.isDirectory()) {

				Project project = models.get(projectDir.getName());
				if (project == null) {

					File bnd = new File(projectDir, Project.BNDFILE);
					if (bnd.isFile()) {

						project = new Project(workspace, projectDir);
						if (project.isValid()) {
							models.put(project.getName(), project);
							workspace.addClose(project);
						} else {
							IO.close(project);
						}
					}
				} else {
					older.remove(projectDir.getName());
				}
			}

		}
		models.entrySet()
			.stream()
			.filter(e -> older.contains(e.getKey()))
			.map(Map.Entry::getValue)
			.forEach(project -> {
				workspace.removeClose(project);
				IO.close(project);
			});
	}
}
