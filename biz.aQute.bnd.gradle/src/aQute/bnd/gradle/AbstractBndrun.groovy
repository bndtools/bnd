package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Processor
import aQute.bnd.repository.fileset.FileSetRepository
import aQute.lib.io.IO

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction

/**
 * Asbtract Bndrun task type for Gradle.
 *
 * <p>
 * This abstract task type is the super type of the bndrun based task types.
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bndrun - This is the bndrun file to be run.
 * This property must be set.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd Workspace builds. The default is
 * "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files".
 * This must not be used for Bnd Workspace builds.</li>
 * <li>ignoreFailures - If true the task will not fail if the execution
 * fails. The default is false.</li>
 * <li>workingDirectory - This is the directory for the execution.
 * The default for workingDirectory is temporaryDir.</li>
 * </ul>
 */
public abstract class AbstractBndrun extends DefaultTask {
	/**
	 * The bndrun file for the execution.
	 *
	 */
	@InputFile
	@PathSensitive(RELATIVE)
	final RegularFileProperty bndrun
	
	/**
	 * The bundles to added to a FileSetRepository for non-Bnd Workspace builds.
	 * <p>
	 * This must not be used for Bnd Workspace builds.
	 */
	@InputFiles
	@PathSensitive(RELATIVE)
	final ConfigurableFileCollection bundles
	
	/**
	 * Whether execution failures should be ignored.
	 *
	 * <p>
	 * If <code>true</code>, then execution failures will not fail the task.
	 * Otherwise, a execution failure will fail the task. The default is
	 * <code>false</code>.
	 */
	@Input
	boolean ignoreFailures = false

	/**
	 * The working directory for the execution.
	 *
	 */
	@Internal
	final DirectoryProperty workingDirectory

	/**
	 * Create a Bndrun task.
	 *
	 */
	public AbstractBndrun() {
		super()
		Project project = getProject()
		ObjectFactory objects = project.getObjects()
		bndrun = objects.fileProperty()
		DirectoryProperty temporaryDirProperty = objects.directoryProperty()
		temporaryDirProperty.set(getTemporaryDir())
		workingDirectory = objects.directoryProperty().convention(temporaryDirProperty)
		bundles = objects.fileCollection()
		if (project.hasProperty("bndWorkspace")) {
			bundles.disallowChanges() // bundles must not be used for Bnd workspace builds
		} else {
			bundles(project.sourceSets.main.getRuntimeClasspath())
			bundles(project.getConfigurations().archives.getArtifacts().getFiles())
			// We add this in case someone actually looks for this convention
			getConvention().getPlugins().put("bundles", new FileSetRepositoryConvention(this))
		}
	}

	/**
	 * Add files to use when locating bundles.
	 *
	 * <p>
	 * The arguments will be handled using
	 * ConfigurableFileCollection.from().
	 */
	public ConfigurableFileCollection bundles(Object... paths) {
		return builtBy(getBundles().from(paths), paths)
	}

	/**
	 * Set the files to use when locating bundles.
	 *
	 * <p>
	 * The argument will be handled using
	 * ConfigurableFileCollection.from().
	 */
	public void setBundles(Object path) {
		getBundles().setFrom(Collections.emptyList())
		getBundles().setBuiltBy(Collections.emptyList())
		bundles(path)
	}

	/**
	 * Setup the Run object and call worker on it.
	 */
	@TaskAction
	void bndrunAction() {
		var workspace = getProject().findProperty("bndWorkspace")
		File bndrunFile = unwrap(getBndrun())
		File workingDirFile = unwrap(getWorkingDirectory())
		if (Objects.nonNull(workspace) && getProject().getPlugins().hasPlugin(BndPlugin.PLUGINID) && Objects.equals(bndrunFile, getProject().bnd.project.getPropertiesFile())) {
			worker(getProject().bnd.project)
			return
		}
		try (var run = createRun(workspace, bndrunFile)) {
			var runWorkspace = run.getWorkspace()
			IO.mkdirs(workingDirFile)
			if (Objects.isNull(workspace)) {
				Properties gradleProperties = new PropertiesWrapper(runWorkspace.getProperties())
				gradleProperties.put("task", this)
				gradleProperties.put("project", getProject())
				run.setParent(new Processor(runWorkspace, gradleProperties, false))
			}
			run.setBase(workingDirFile)
			if (run.isStandalone()) {
				runWorkspace.setOffline(Objects.nonNull(workspace) ? workspace.isOffline() : getProject().getGradle().getStartParameter().isOffline())
				File cnf = new File(workingDirFile, Workspace.CNFDIR)
				IO.mkdirs(cnf)
				runWorkspace.setBuildDir(cnf)
				if (Objects.isNull(workspace)) {
					FileSetRepository fileSetRepository = new FileSetRepository(name, getBundles().getFiles())
					runWorkspace.addBasicPlugin(fileSetRepository)
					runWorkspace.getRepositories().forEach(repo -> repo.list(null))
				}
			}
			run.getInfo(runWorkspace)
			logReport(run, getLogger())
			if (!run.isOk()) {
				throw new GradleException("${run.getPropertiesFile()} workspace errors")
			}

			worker(run)
		}
	}

	/**
	 * Create the Run object.
	 */
	protected Object createRun(var workspace, File bndrunFile) {
		Class runClass = workspace ? Class.forName(aQute.bnd.build.Run.class.getName(), true, workspace.getClass().getClassLoader()) : aQute.bnd.build.Run.class
		return runClass.createRun(workspace, bndrunFile)
	}

	/**
	 * Execute the Run object.
	 */
	abstract protected void worker(var run)
}
