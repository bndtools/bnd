package aQute.bnd.gradle;

import static aQute.bnd.build.Project.BNDFILE;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static aQute.bnd.gradle.BndUtils.unwrapOptional;

import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import aQute.bnd.unmodifiable.Maps;

/**
 * BndBuilderPlugin for Gradle.
 * <p>
 * The plugin name is {@code biz.aQute.bnd.builder}.
 * <p>
 * This plugin applies the java plugin to a project and modifies the jar task by
 * adding the {@code bundle} extension {@link BundleTaskExtension}, setting the
 * bndfile to "bnd.bnd", if the file exists, and building the jar file as a
 * bundle.
 * <p>
 * This plugin also defines a "baseline" configuration and a baseline task of
 * type {@link Baseline}. The baseline task will be set up with the default of
 * baselining the output of the jar task using the baseline configuration. The
 * baseline configuration default dependency will use the prior version of the
 * jar.
 */
public class BndBuilderPlugin implements Plugin<Project> {
	/**
	 * Name of the plugin.
	 */
	public static final String	PLUGINID		= "biz.aQute.bnd.builder";

	/**
	 * Apply the {@code biz.aQute.bnd.builder} plugin to the specified project.
	 */
	@Override
	public void apply(Project project) {
		if (project.getPluginManager()
			.hasPlugin(BndPlugin.PLUGINID)) {
			throw new GradleException("Project already has \"" + BndPlugin.PLUGINID + "\" plugin applied.");
		}
		project.getPluginManager()
			.apply("java");

		RegularFile defaultBndfile = project.getLayout()
			.getProjectDirectory()
			.file(BNDFILE);

		TaskContainer tasks = project.getTasks();

		@SuppressWarnings("deprecation")
		TaskProvider<Jar> jar = tasks.named(JavaPlugin.JAR_TASK_NAME, Jar.class, t -> {
			t.setDescription("Assembles a bundle containing the main classes.");
			BundleTaskExtension extension = t.getExtensions()
				.create(BundleTaskExtension.NAME, BundleTaskExtension.class, t);
			t.getConvention()
				.getPlugins()
				.put(BundleTaskExtension.NAME, new BundleTaskConvention(extension));
			if (unwrapFile(defaultBndfile).isFile()) {
				extension.getBndfile()
					.convention(defaultBndfile);
			}
			t.doLast("buildBundle", extension.buildAction());
		});

		Configuration baseline = project.getConfigurations()
			.create("baseline");
		baseline.getDependencies()
			.all(dep -> {
				if (dep instanceof ExternalDependency) {
					((ExternalDependency) dep).version(mvc -> mvc.strictly(dep.getVersion()));
				}
				if (dep instanceof ModuleDependency) {
					((ModuleDependency) dep).setTransitive(false);
				}
			});

		TaskProvider<Baseline> baselineTask = tasks.register("baseline", Baseline.class, t -> {
			t.setDescription("Baseline the project bundle.");
			t.setGroup("release");
			t.setBundle(jar);
			t.setBaseline(baseline);
		});

		baseline.defaultDependencies(deps -> {
			Baseline task = unwrap(baselineTask);
			Jar bundleTask = task.getBundleTask();
			if (Objects.nonNull(bundleTask)) {
				String archiveBaseName = unwrap(bundleTask.getArchiveBaseName());
				String archiveVersion = unwrapOptional(bundleTask.getArchiveVersion()).orElse(null);
				String group = project.getGroup()
					.toString();
				task.getLogger()
					.debug("Searching for default baseline {}:{}:(0,{}[", group, archiveBaseName, archiveVersion);
				Dependency baselineDep = project.getDependencies()
					.create(Maps.of("group", group, "name", archiveBaseName));

				((ExternalDependency) baselineDep)
					.version(mvc -> mvc.strictly(String.format("(0,%s[", archiveVersion)));
				((ExternalDependency) baselineDep).setTransitive(false);
				try {
					Configuration detached = project.getConfigurations()
						.detachedConfiguration(baselineDep);
					detached.getResolvedConfiguration()
						.rethrowFailure();
				} catch (ResolveException e) {
					task.getLogger()
						.debug("Baseline configuration resolve error {}, adding {} as baseline", e,
							unwrapFile(task.getBundle()), e);
					baselineDep = project.getDependencies()
						.create(project.getObjects()
							.fileCollection()
							.from(task.getBundle()));
				}
				deps.add(baselineDep);
			}
		});
	}
}
