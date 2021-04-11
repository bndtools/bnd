/**
 * OSGi Bndrun task type for Gradle.
 *
 * <p>
 * This task type can be used to execute a bndrun file.
 *
 * <p>
 * Here is examples of using the Bndrun task type:
 * <pre>
 * import aQute.bnd.gradle.Bndrun
 * tasks.register('run', Bndrun) {
 *   bndrun = file('my.bndrun')
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the execution
 * fails. The default is false.</li>
 * <li>bndrun - This is the bndrun file to be run.
 * This property must be set.</li>
 * <li>workingDirectory - This is the directory for the execution.
 * The default for workingDirectory is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * 'sourceSets.main.runtimeClasspath' plus
 * 'configurations.archives.artifacts.files'.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledExecutorService

import aQute.bnd.build.ProjectLauncher
import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Processor
import aQute.lib.io.IO

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction

public class Bndrun extends DefaultTask {
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
	 * The bndrun file for the execution.
	 *
	 */
	@InputFile
	@PathSensitive(RELATIVE)
	final RegularFileProperty bndrun

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
	public Bndrun() {
		super()
		ObjectFactory objects = getProject().getObjects()
		bndrun = objects.fileProperty()
		DirectoryProperty temporaryDirProperty = objects.directoryProperty()
		temporaryDirProperty.set(getTemporaryDir())
		workingDirectory = objects.directoryProperty().convention(temporaryDirProperty)
		if (!getProject().hasProperty('bndWorkspace')) {
			getConvention().getPlugins().bundles = new FileSetRepositoryConvention(this)
		}
	}

	/**
	 * Set the bndfile for the execution.
	 *
	 * <p>
	 * The argument will be handled using
	 * project.layout.projectDirectory.file().
	 */
	public void setBndrun(String file) {
		getBndrun().value(getProject().getLayout().getProjectDirectory().file(file))
	}

	/**
	 * Set the bndfile for the execution.
	 *
	 * <p>
	 * The argument will be handled using
	 * Project.file().
	 */
	public void setBndrun(Object file) {
		getBndrun().set(getProject().file(file))
	}

	@Deprecated
	@ReplacedBy('workingDirectory')
	public File getWorkingDir() {
		return unwrap(getWorkingDirectory())
	}

	@Deprecated
	public void setWorkingDir(Object dir) {
		getWorkingDirectory().set(getProject().file(dir))
	}

	/**
	 * Setup the Run object and call worker on it.
	 */
	@TaskAction
	void bndrunAction() {
		var workspace = getProject().findProperty('bndWorkspace')
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
				gradleProperties.put('task', this)
				gradleProperties.put('project', getProject())
				run.setParent(new Processor(runWorkspace, gradleProperties, false))
			}
			run.setBase(workingDirFile)
			if (run.isStandalone()) {
				runWorkspace.setOffline(Objects.nonNull(workspace) ? workspace.isOffline() : getProject().getGradle().getStartParameter().isOffline())
				File cnf = new File(workingDirFile, Workspace.CNFDIR)
				IO.mkdirs(cnf)
				runWorkspace.setBuildDir(cnf)
				if (getConvention().findPlugin(FileSetRepositoryConvention)) {
					runWorkspace.addBasicPlugin(getFileSetRepository(name))
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
	protected void worker(var run) {
		getLogger().info('Running {} in {}', run.getPropertiesFile(), run.getBase())
		getLogger().debug('Run properties: {}', run.getProperties())
		ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
		try (ProjectLauncher pl = run.getProjectLauncher();
		ProjectLauncher.LiveCoding lc = pl.liveCoding(ForkJoinPool.commonPool(), scheduledExecutor)) {
			pl.setTrace(run.isTrace() || run.isRunTrace())
			pl.launch()
		} finally {
			scheduledExecutor.shutdownNow()
			logReport(run, getLogger())
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException("${run.getPropertiesFile()} execution failure")
		}
	}
}
