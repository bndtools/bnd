package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.unwrap
import static aQute.bnd.build.Project.BNDFILE

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.TaskContainer

/**
 * BndBuilderPlugin for Gradle.
 *
 * <p>
 * The plugin name is {@code biz.aQute.bnd.builder}.
 *
 * <p>
 * This plugin applies the java plugin to a project and modifies the jar
 * task by adding the {@code bundle} extension {@link BundleTaskExtension},
 * setting the bndfile to "bnd.bnd", if the file exists, and building the
 * jar file as a bundle.
 * <p>
 * This plugin also defines a "baseline" configuration and a baseline task
 * of type {@link Baseline}. The baseline task will be set up with the
 * default of baselining the output of the jar task using the baseline
 * configuration. The baseline configuration default dependency
 * will use the prior version of the jar.
 */
public class BndBuilderPlugin implements Plugin<Project> {
	public static final String PLUGINID = "biz.aQute.bnd.builder"

	/**
	 * Apply the {@code biz.aQute.bnd.builder} plugin to the specified project.
	 */
	@Override
	public void apply(Project project) {
		if (project.getPlugins().hasPlugin(BndPlugin.PLUGINID)) {
			throw new GradleException("Project already has \"${BndPlugin.PLUGINID}\" plugin applied.")
		}
		project.getPlugins().apply("java")

		TaskContainer tasks = project.getTasks()

		var jar = tasks.named("jar", t -> {
			t.setDescription("Assembles a bundle containing the main classes.")
			BundleTaskExtension extension = t.getExtensions().create(BundleTaskExtension.NAME, BundleTaskExtension.class, t)
			t.getConvention().getPlugins().put(BundleTaskExtension.NAME, new BundleTaskConvention(extension, t))
			RegularFile defaultBndfile = project.getLayout().getProjectDirectory().file(BNDFILE)
			if (defaultBndfile.getAsFile().isFile()) {
				extension.getBndfile().convention(defaultBndfile)
			}
			t.doLast("buildBundle", tt -> extension.buildBundle())
		})

		Configuration baseline = project.getConfigurations().create("baseline")
		baseline.getDependencies().all((Dependency dep) -> {
			if (dep instanceof ExternalDependency) {
				dep.version(mvc -> mvc.strictly(dep.getVersion()))
			}
			if (dep instanceof ModuleDependency) {
				dep.setTransitive(false)
			}
		})

		tasks.register("baseline", Baseline.class, t -> {
			t.setDescription("Baseline the project bundle.")
			t.setGroup("release")
			t.bundle = jar
			t.baseline = baseline
		})

		baseline.defaultDependencies((DependencySet deps) -> {
			Task baselineTask = tasks.getByName("baseline")
			Task bundleTask = baselineTask.getBundleTask()
			if (bundleTask) {
				String archiveBaseName = unwrap(bundleTask.getArchiveBaseName())
				String archiveVersion = unwrap(bundleTask.getArchiveVersion(), true)
				String group = project.getGroup().toString()
				baselineTask.getLogger().debug("Searching for default baseline {}:{}:(0,{}[", group, archiveBaseName, archiveVersion)
				Dependency baselineDep = project.getDependencies().create("group": group, "name": archiveBaseName) {
					version {
						strictly("(0,${archiveVersion}[")
					}
					transitive = false
				}
				try {
					Configuration detached = project.getConfigurations().detachedConfiguration(baselineDep)
					detached.getResolvedConfiguration().rethrowFailure()
				} catch(ResolveException e) {
					baselineTask.getLogger().debug("Baseline configuration resolve error {}, adding {} as baseline", e, unwrap(baselineTask.getBundle()), e)
					baselineDep = project.getDependencies().create(project.getObjects().fileCollection().from(baselineTask.getBundle()))
				}
				deps.add(baselineDep)
			}
		})
	}
}
