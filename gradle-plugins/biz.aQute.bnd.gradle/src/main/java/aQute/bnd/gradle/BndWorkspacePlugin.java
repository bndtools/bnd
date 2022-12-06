package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.unwrapFile;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Constants;
import aQute.bnd.unmodifiable.Sets;
import aQute.lib.strings.Strings;
import groovy.lang.Closure;
import org.gradle.StartParameter;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.Delete;
import org.gradle.internal.metaobject.DynamicInvokeResult;
import org.gradle.internal.metaobject.DynamicObject;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * BndWorkspacePlugin for Gradle.
 * <p>
 * The plugin name is {@code biz.aQute.bnd.workspace}.
 * <p>
 * This plugin can be applied to the Settings object in settings.gradle. It can
 * also be applied to the root project in build.gradle.
 */
public class BndWorkspacePlugin implements Plugin<Object> {
	/**
	 * Name of the plugin.
	 */
	public static final String			PLUGINID			= "biz.aQute.bnd.workspace";

	private static final Pattern		OPTION_P			= Pattern.compile("--(?<option>\\w+)(?:=(?<value>\\S+)?)?");
	private static final Pattern		TASKNAME_SPLITTER	= Pattern.compile(":");
	private static final Set<String>	SPECIAL_FOLDERS		= Sets.of("buildSrc", "gradle");

	/**
	 * Default public constructor.
	 */
	public BndWorkspacePlugin() {}

	/**
	 * Apply the {@code biz.aQute.bnd.workspace} plugin.
	 */
	@Override
	public void apply(Object target) {
		try {
			if (target instanceof Settings settings) {
				configureSettings(settings);
			} else if (target instanceof Project project) {
				configureWorkspaceProject(project);
			} else {
				throw new GradleException(String.format("The target %s is not a Settings or a Project", target));
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void configureSettings(Settings settings) throws Exception {
		DynamicObject dynamicObject = new DslObject(settings).getAsDynamicObject();

		/* Start with the declared build project name */
		DynamicInvokeResult result = dynamicObject.tryGetProperty("bnd_build");
		String bnd_build = result.isFound() ? (String) result.getValue() : "";
		String defaultProjectName = bnd_build;

		/* If in a subproject, use the subproject name */
		StartParameter startParameter = settings.getStartParameter();
		File rootDir = settings.getRootDir();
		for (File currentDir = startParameter.getCurrentDir(); !Objects.equals(currentDir,
			rootDir); currentDir = currentDir.getParentFile()) {
			defaultProjectName = currentDir.getName();
		}

		/*
		 * Build a set of project names we need to include from the specified
		 * tasks
		 */
		Set<String> projectNames = new LinkedHashSet<>();
		for (Iterator<String> iter = startParameter.getTaskNames()
			.iterator(); iter.hasNext();) {
			String taskName = iter.next();
			Matcher m = OPTION_P.matcher(taskName);
			if (m.matches()) {
				taskName = m.group("value"); // option value may be a task name
				if (Objects.equals(TestOSGi.OPTION_TESTS, m.group("option"))) {
					if (Objects.isNull(taskName) && iter.hasNext()) {
						iter.next();
					}
					continue; // ignore --tests value
				}
				if (Objects.isNull(taskName)) {
					continue; // no task name to process
				}
			}
			String[] elements = TASKNAME_SPLITTER.split(taskName);
			projectNames.add(switch (elements.length) {
				case 1 -> defaultProjectName;
				case 2 -> elements[0].isEmpty() ? bnd_build : elements[0];
				default -> elements[0].isEmpty() ? elements[1] : elements[0];
			});
		}

		/*
		 * Include the default project name if in a subproject or no tasks
		 * specified
		 */
		if (!Objects.equals(startParameter.getCurrentDir(), rootDir) || projectNames.isEmpty()) {
			projectNames.add(defaultProjectName);
		}

		/*
		 * If build used but empty, add all non-private folders of rootDir
		 * except special gradle folders
		 */
		if (projectNames.remove("")) {
			for (File dir : rootDir.listFiles(File::isDirectory)) {
				String projectName = dir.getName();
				if (projectName.startsWith(".") || SPECIAL_FOLDERS.contains(projectName)) {
					continue;
				}
				projectNames.add(projectName);
			}
		}

		/* Add cnf project to the graph */
		result = dynamicObject.tryGetProperty("bnd_cnf");
		String cnf = result.isFound() ? (String) result.getValue() : Workspace.CNFDIR;
		projectNames.add(cnf);

		/* Add any projects which must always be included */
		result = dynamicObject.tryGetProperty("bnd_include");
		if (result.isFound()) {
			Strings.splitAsStream((String) result.getValue())
				.forEach(projectNames::add);
		}

		/* Remove any projects which must always be excluded */
		result = dynamicObject.tryGetProperty("bnd_exclude");
		if (result.isFound()) {
			Strings.splitAsStream((String) result.getValue())
				.forEach(projectNames::remove);
		}

		/* Remove any projects which are composite builds */
		projectNames.removeIf(projectName -> {
			File projectDir = new File(rootDir, projectName);
			return new File(projectDir, Settings.DEFAULT_SETTINGS_FILE).isFile()
				|| new File(projectDir, Settings.DEFAULT_SETTINGS_FILE + ".kts").isFile();
		});

		/* Initialize the Bnd workspace */
		Workspace.setDriver(Constants.BNDDRIVER_GRADLE);
		Workspace.addGestalt(Constants.GESTALT_BATCH, null);
		Workspace workspace = new Workspace(rootDir, cnf);
		workspace.setOffline(startParameter.isOffline());
		Gradle gradle = settings.getGradle();
		bndWorkspaceConfigure(workspace, gradle);

		/*
		 * Prepare each project in the workspace to establish complete
		 * dependencies and dependents information.
		 */
		for (aQute.bnd.build.Project p : workspace.getAllProjects()) {
			p.prepare();
		}

		/* Add each project and its dependents to the graph */
		Set<String> projectGraph = new LinkedHashSet<>();
		while (!projectNames.isEmpty()) {
			String projectName = projectNames.iterator()
				.next();
			projectGraph.add(projectName);
			aQute.bnd.build.Project p = workspace.getProject(projectName);
			if (Objects.nonNull(p)) {
				p.getDependents()
					.stream()
					.map(aQute.bnd.build.Project::getName)
					.forEach(projectNames::add);
			}
			projectNames.removeAll(projectGraph);
		}

		/* Add each project and its dependencies to the graph */
		projectNames = projectGraph;
		projectGraph = new LinkedHashSet<>();
		while (!projectNames.isEmpty()) {
			String projectName = projectNames.iterator()
				.next();
			projectGraph.add(projectName);
			aQute.bnd.build.Project p = workspace.getProject(projectName);
			if (Objects.nonNull(p)) {
				p.getTestDependencies()
					.stream()
					.map(aQute.bnd.build.Project::getName)
					.forEach(projectNames::add);
			}
			projectNames.removeAll(projectGraph);
		}

		settings.include(projectGraph.toArray(new String[0]));

		/* Apply workspace plugin to root project */
		gradle.rootProject(project -> {
			ExtraPropertiesExtension ext = project.getExtensions()
				.getExtraProperties();
			ext.set("bnd_cnf", cnf);
			ext.set("bndWorkspace", workspace);
			project.getPluginManager()
				.apply(BndWorkspacePlugin.class);
		});
	}

	private void configureWorkspaceProject(Project workspace) throws Exception {
		Workspace bndWorkspace = getBndWorkspace(workspace);

		/* Configure the Bnd projects */
		for (Project project : workspace.getSubprojects()) {
			if (Objects.nonNull(bndWorkspace.getProject(project.getName()))) {
				project.getPluginManager()
					.apply(BndPlugin.class);
			}
		}
	}

	/**
	 * Return the Bnd Workspace for the specified Gradle project.
	 *
	 * @param workspace The Gradle project for the workspace.
	 * @return The Bnd Workspace for the specified Gradle project.
	 * @throws Exception An exception while finding the Bnd Workspace.
	 */
	public static Workspace getBndWorkspace(Project workspace) throws Exception {
		ExtraPropertiesExtension ext = workspace.getExtensions()
			.getExtraProperties();
		/* Initialize the Bnd workspace */
		String bnd_cnf = (String) workspace.findProperty("bnd_cnf");
		if (Objects.isNull(bnd_cnf)) {
			// if not passed from settings
			bnd_cnf = Workspace.CNFDIR;
			ext.set("bnd_cnf", bnd_cnf);
		}
		Workspace bndWorkspace = (Workspace) workspace.findProperty("bndWorkspace");
		if (Objects.isNull(bndWorkspace)) {
			// if not passed from settings
			Workspace.setDriver(Constants.BNDDRIVER_GRADLE);
			Workspace.addGestalt(Constants.GESTALT_BATCH, null);
			Gradle gradle = workspace.getGradle();
			File rootDir = unwrapFile(workspace.getLayout()
				.getProjectDirectory());
			bndWorkspace = new Workspace(rootDir, bnd_cnf);
			bndWorkspace.setOffline(gradle.getStartParameter()
				.isOffline());
			ext.set("bndWorkspace", bndWorkspace);
			bndWorkspaceConfigure(bndWorkspace, gradle);
		}

		/* Configure cnf project */
		Project cnfProject = (Project) workspace.findProperty("cnf");
		if (Objects.isNull(cnfProject)) {
			cnfProject = workspace.findProject(bnd_cnf);
			if (Objects.nonNull(cnfProject)) {
				ext.set("cnf", cnfProject);
				Directory cacheDir = cnfProject.getLayout()
					.getProjectDirectory()
					.dir("cache");
				cnfProject.getTasks()
					.register("cleanCache", Delete.class, t -> {
						t.setDescription("Clean the cnf cache folder.");
						t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
						t.delete(cacheDir);
					});
			}
		}

		return bndWorkspace;
	}

	private static void bndWorkspaceConfigure(Workspace workspace, Gradle gradle) {
		ExtraPropertiesExtension ext = new DslObject(gradle).getExtensions()
			.getExtraProperties();
		if (ext.has("bndWorkspaceConfigure")) {
			Object bndWorkspaceConfigure = ext.get("bndWorkspaceConfigure");
			if (bndWorkspaceConfigure instanceof Closure<?> closure) {
				closure.call(workspace);
			} else if (bndWorkspaceConfigure instanceof Action) {
				@SuppressWarnings("unchecked")
				Action<Workspace> action = (Action<Workspace>) bndWorkspaceConfigure;
				action.execute(workspace);
			} else {
				throw new GradleException(
					String.format("The bndWorkspaceConfigure %s is not a Closure or an Action", bndWorkspaceConfigure));
			}
		}
	}
}
