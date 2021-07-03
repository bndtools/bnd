package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledExecutorService

import aQute.bnd.build.ProjectLauncher

import org.gradle.api.GradleException

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
 * tasks.register("run", Bndrun) {
 *   bndrun = file("my.bndrun")
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
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd Workspace builds. The default is
 * "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files".
 * This must not be used for Bnd Workspace builds.</li>
 * </ul>
 */
public class Bndrun extends AbstractBndrun {
	/**
	 * Create a Bndrun task.
	 *
	 */
	public Bndrun() {
		super()
	}

	/**
	 * Execute the Run object.
	 */
	@Override
	protected void worker(var run) {
		getLogger().info("Running {} in {}", run.getPropertiesFile(), run.getBase())
		getLogger().debug("Run properties: {}", run.getProperties())
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
