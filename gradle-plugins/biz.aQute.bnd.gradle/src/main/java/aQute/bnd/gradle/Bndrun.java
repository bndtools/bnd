package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.defaultToolFor;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.lib.io.IO;

/**
 * OSGi Bndrun task type for Gradle.
 * <p>
 * This task type can be used to execute a bndrun file.
 * <p>
 * Here is examples of using the Bndrun task type:
 *
 * <pre>
 * import aQute.bnd.gradle.Bndrun
 * tasks.register("run", Bndrun) {
 *   bndrun = file("my.bndrun")
 * }
 * </pre>
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the execution fails.
 * The default is false.</li>
 * <li>bndrun - This is the bndrun file to be run. This property must be
 * set.</li>
 * <li>workingDirectory - This is the directory for the execution. The default
 * for workingDirectory is temporaryDir.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd
 * Workspace builds. The default is "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files". This must not be used for Bnd
 * Workspace builds.</li>
 * <li>javaLauncher - Configures the default java executable to be used for
 * execution.</li>
 * </ul>
 */
public class Bndrun extends AbstractBndrun {
	private final Property<JavaLauncher> javaLauncher;

	/**
	 * Configures the default java executable to be used for execution.
	 * <p>
	 * This java launcher is used if the bndrun does not specify the
	 * {@code java} property or specifies it with the default value
	 * {@code java}.
	 *
	 * @return The JavaLauncher property.
	 */
	@Nested
	@Optional
	public Property<JavaLauncher> getJavaLauncher() {
		return javaLauncher;
	}

	/**
	 * Create a Bndrun task.
	 */
	public Bndrun() {
		super();
		ObjectFactory objects = getProject().getObjects();
		javaLauncher = objects.property(JavaLauncher.class)
			.convention(defaultToolFor(getProject(), JavaToolchainService::launcherFor));
	}

	/**
	 * Execute the Project object.
	 *
	 * @param run The Project object.
	 * @throws Exception If the worker action has an exception.
	 */
	@Override
	protected void worker(Project run) throws Exception {
		if (getJavaLauncher().isPresent() && Objects.equals(run.getProperty("java", "java"), "java")) {
			run.setProperty("java", IO.absolutePath(unwrapFile(unwrap(getJavaLauncher()).getExecutablePath())));
		}
		getLogger().info("Running {} in {}", run.getPropertiesFile(), run.getBase());
		getLogger().debug("Run properties: {}", run.getProperties());
		ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		try (ProjectLauncher pl = run.getProjectLauncher();
			ProjectLauncher.LiveCoding lc = pl.liveCoding(ForkJoinPool.commonPool(), scheduledExecutor)) {
			pl.setTrace(run.isTrace() || run.isRunTrace());
			pl.launch();
		} finally {
			scheduledExecutor.shutdownNow();
			logReport(run, getLogger());
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException(String.format("%s execution failure", run.getPropertiesFile()));
		}
	}
}
