package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.unwrap

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants

import org.gradle.StartParameter
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.Delete

/**
 * BndWorkspacePlugin for Gradle.
 *
 * <p>
 * The plugin name is {@code biz.aQute.bnd.workspace}.
 * <p>
 * This plugin can be applied to the Settings object in
 * settings.gradle. It can also be applied to the root
 * project in build.gradle.
 *
 */
public class BndWorkspacePlugin implements Plugin<Object> {
	public static final String PLUGINID = "biz.aQute.bnd.workspace"
	/**
	 * Apply the {@code biz.aQute.bnd.workspace} plugin.
	 */
	@Override
	public void apply(Object target) {
		if (target instanceof Settings) {
			configureSettings(target)
		} else if (target instanceof Project) {
			configureWorkspaceProject(target)
		} else {
			throw new GradleException("The target ${target} is not a Settings or a Project")
		}
	}

	private void configureSettings(Settings settings) {
		/* Start with the declared build project name */
		String build = ""
		try {
			build = settings.bnd_build
		} catch (MissingPropertyException mpe) {}
		String defaultProjectName = build

		/* If in a subproject, use the subproject name */
		StartParameter startParameter = settings.getStartParameter()
		File rootDir = settings.getRootDir()
		for (File currentDir = startParameter.getCurrentDir(); !Objects.equals(currentDir, rootDir); currentDir = currentDir.getParentFile()) {
			defaultProjectName = currentDir.getName()
		}

		/* Build a set of project names we need to include from the specified tasks */
		Set<String> projectNames = new LinkedHashSet<>()
		for (Iterator<String> iter = startParameter.getTaskNames().iterator(); iter.hasNext();) {
			String taskName = iter.next()
			if (Objects.equals(taskName, "--tests")) {
				if (iter.hasNext()) {
					iter.next()
				}
				continue
			}
			String[] elements = taskName.split(":")
			switch (elements.length) {
				case 1:
					projectNames.add(defaultProjectName)
					break
				case 2:
					projectNames.add(elements[0].isEmpty() ? build : elements[0])
					break
				default:
					projectNames.add(elements[0].isEmpty() ? elements[1] : elements[0])
					break
			}
		}

		/* Include the default project name if in a subproject or no tasks specified */
		if (!Objects.equals(startParameter.getCurrentDir(), rootDir) || projectNames.isEmpty()) {
			projectNames.add(defaultProjectName)
		}

		/* If build used but empty, add all non-private folders of rootDir except special gradle folders */
		if (projectNames.remove("")) {
			Set<String> specialFolders = ["buildSrc", "gradle"]
			rootDir.eachDir {
				String projectName = it.getName()
				if (!projectName.startsWith(".") && !specialFolders.contains(projectName)) {
					projectNames.add(projectName)
				}
			}
		}

		/* Add cnf project to the graph */
		String cnf = "cnf"
		try {
			cnf = settings.bnd_cnf.trim()
		} catch (MissingPropertyException mpe) {}
		projectNames.add(cnf)

		/* Add any projects which must always be included */
		try {
			projectNames.addAll(settings.bnd_include.trim().split(/\s*,\s*/))
		} catch (MissingPropertyException mpe) {}

		/* Initialize the Bnd workspace */
		Workspace.setDriver(Constants.BNDDRIVER_GRADLE)
		Workspace.addGestalt(Constants.GESTALT_BATCH, null)
		Workspace workspace = new Workspace(rootDir, cnf).setOffline(startParameter.isOffline())
		Gradle gradle = settings.getGradle()
		if (gradle.ext.has("bndWorkspaceConfigure")) {
			gradle.bndWorkspaceConfigure(workspace)
		}

		/* Prepare each project in the workspace to establish complete 
		 * dependencies and dependents information.
		 */
		workspace.getAllProjects().forEach(p -> p.prepare())

		/* Add each project and its dependents to the graph */
		Set<String> projectGraph = new LinkedHashSet<>()
		while (!projectNames.isEmpty()) {
			String projectName = projectNames.head()
			projectGraph.add(projectName)
			var p = workspace.getProject(projectName)
			if (p) {
				projectNames.addAll(p.getDependents()*.getName())
			}
			projectNames.removeAll(projectGraph)
		}

		/* Add each project and its dependencies to the graph */
		projectNames = projectGraph
		projectGraph = new LinkedHashSet<>()
		while (!projectNames.isEmpty()) {
			String projectName = projectNames.head()
			projectGraph.add(projectName)
			var p = workspace.getProject(projectName)
			if (p) {
				projectNames.addAll(p.getTestDependencies()*.getName())
			}
			projectNames.removeAll(projectGraph)
		}

		settings.include(projectGraph as String[])

		/* Apply workspace plugin to root project */
		gradle.rootProject((Project project) -> {
			project.ext.bnd_cnf = cnf
			project.ext.bndWorkspace = workspace
			project.getPlugins().apply(BndWorkspacePlugin.class)
		})
	}

	private void configureWorkspaceProject(Project workspace) {
		Workspace bndWorkspace = getBndWorkspace(workspace)

		/* Configure the Bnd projects */
		workspace.subprojects((Project project) -> {
			if (Objects.nonNull(bndWorkspace.getProject(project.getName()))) {
				project.getPlugins().apply(BndPlugin.class)
			}
		})
	}

	static Workspace getBndWorkspace(Project workspace) {
		/* Initialize the Bnd workspace */
		String bnd_cnf = workspace.findProperty("bnd_cnf")
		if (Objects.isNull(bnd_cnf)) {
			// if not passed from settings
			workspace.ext.bnd_cnf = bnd_cnf = "cnf"
		}
		Workspace bndWorkspace = workspace.findProperty("bndWorkspace")
		if (Objects.isNull(bndWorkspace)) {
			// if not passed from settings
			Workspace.setDriver(Constants.BNDDRIVER_GRADLE)
			Workspace.addGestalt(Constants.GESTALT_BATCH, null)
			Gradle gradle = workspace.getGradle()
			workspace.ext.bndWorkspace = bndWorkspace = new Workspace(unwrap(workspace.getLayout().getProjectDirectory()), bnd_cnf).setOffline(gradle.getStartParameter().isOffline())
			if (gradle.ext.has("bndWorkspaceConfigure")) {
				gradle.bndWorkspaceConfigure(bndWorkspace)
			}
		}

		/* Configure cnf project */
		Project cnfProject = workspace.findProperty("cnf")
		if (Objects.isNull(cnfProject)) {
			cnfProject = workspace.findProject(bnd_cnf)
			if (Objects.nonNull(cnfProject)) {
				workspace.ext.cnf = cnfProject
				cnfProject.getTasks().register("cleanCache", Delete.class, t -> {
					t.setDescription("Clean the cache folder.")
					t.setGroup("build")
					t.delete(cnfProject.getLayout().getProjectDirectory().dir("cache"))
				})
			}
		}

		return bndWorkspace
	}
}
