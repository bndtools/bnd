package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.unwrap;

import java.io.File;
import java.io.Writer;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import biz.aQute.resolve.ResolveProcess;

/**
 * Resolve task type for Gradle.
 * <p>
 * This task type can be used to resolve a bndrun file setting the `-runbundles`
 * instruction.
 * <p>
 * Here is an example of using the Resolve task type:
 *
 * <pre>
 * import aQute.bnd.gradle.Resolve
 * def resolveTask = tasks.register("resolve", Resolve) {
 *   bndrun = file("my.bndrun")
 *   outputBndrun = layout.buildDirectory.file("my.bndrun")
 * }
 * </pre>
 * <p>
 * Properties:
 * <ul>
 * <li>bndrun - This is the bndrun file to be resolved. This property must be
 * set.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd
 * Workspace builds. The default is "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files". This must not be used for Bnd
 * Workspace builds.</li>
 * <li>ignoreFailures - If true the task will not fail if the execution fails.
 * The default is false.</li>
 * <li>workingDirectory - This is the directory for the resolve process. The
 * default for workingDirectory is temporaryDir.</li>
 * <li>failOnChanges - If true the task will fail if the resolve process results
 * in a different value for -runbundles than the current value. The default is
 * false.</li>
 * <li>outputBndrun - This is the output file for the calculated -runbundles
 * property. The default is the input bndrun file which means the input bndrun
 * file will be updated in place.</li>
 * <li>reportOptional - If true failure reports will include optional
 * requirements. The default is true.</li>
 * <li>writeOnChanges - If true the task will write changes to the value of the
 * -runbundles property. The default is true.</li>
 * </ul>
 */
public class Resolve extends AbstractBndrun<biz.aQute.resolve.Bndrun, biz.aQute.resolve.Bndrun> {
	private boolean						failOnChanges	= false;
	private final RegularFileProperty	outputBndrun;
	private boolean						reportOptional	= true;
	private boolean						writeOnChanges	= true;

	/**
	 * Whether resolve changes should fail the task.
	 *
	 * @return <code>true</code> if a change to the current
	 *         <code>-runbundles</code> value will fail the task. The default is
	 *         <code>false</code>.
	 */
	@Input
	public boolean isFailOnChanges() {
		return failOnChanges;
	}

	/**
	 * Whether resolve changes should fail the task.
	 * <p>
	 * Alias for {@link #isFailOnChanges()}.
	 *
	 * @return <code>true</code> if a change to the current
	 *         <code>-runbundles</code> value will fail the task. The default is
	 *         <code>false</code>.
	 */
	@Internal
	public boolean getFailOnChanges() {
		return isFailOnChanges();
	}

	/**
	 * Set whether resolve changes should fail the task.
	 *
	 * @param failOnChanges If <code>true</code>, then a change to the current
	 *            <code>-runbundles</code> value will fail the task. The default
	 *            is <code>false</code>.
	 */
	public void setFailOnChanges(boolean failOnChanges) {
		this.failOnChanges = failOnChanges;
	}

	/**
	 * Return the output file for the calculated <code>-runbundles</code>
	 * property.
	 * <p>
	 * By default, the input <code>bndrun</code> file is used as the output
	 * bndrun file. That is, the input bndrun file will be updated in place. If
	 * this property is set to a value other than the input bndrun file, the
	 * output bndrun file will <code>-include</code> the input bndrun file and
	 * can be thus be used by other tasks, such as TestOSGi as a resolved input
	 * bndrun file.
	 *
	 * @return The output file for the calculated <code>-runbundles</code>
	 *         property.
	 */
	@OutputFile
	public RegularFileProperty getOutputBndrun() {
		return outputBndrun;
	}

	/**
	 * Whether to report optional requirements.
	 *
	 * @return <code>true</code> if optional requirements will be reported. The
	 *         default is <code>false</code>.
	 */
	@Input
	public boolean isReportOptional() {
		return reportOptional;
	}

	/**
	 * Whether to report optional requirements.
	 * <p>
	 * Alias for {@link #isReportOptional()}.
	 *
	 * @return <code>true</code> if optional requirements will be reported. The
	 *         default is <code>false</code>.
	 */
	@Internal
	public boolean getReportOptional() {
		return isReportOptional();
	}

	/**
	 * Set whether to report optional requirements.
	 *
	 * @param reportOptional If <code>true</code>, then optional requirements
	 *            will be reported. The default is <code>false</code>.
	 */
	public void setReportOptional(boolean reportOptional) {
		this.reportOptional = reportOptional;
	}

	/**
	 * Whether resolve changes should be written.
	 *
	 * @return <code>true</code> if a change to the current
	 *         <code>-runbundles</code> value will be written to the output
	 *         bndrun file. The default is <code>false</code>.
	 */
	@Input
	public boolean isWriteOnChanges() {
		return writeOnChanges;
	}

	/**
	 * Whether resolve changes should be written.
	 * <p>
	 * Alias for {@link #isWriteOnChanges()}.
	 *
	 * @return <code>true</code> if a change to the current
	 *         <code>-runbundles</code> value will be written to the output
	 *         bndrun file. The default is <code>false</code>.
	 */
	@Internal
	public boolean getWriteOnChanges() {
		return isWriteOnChanges();
	}

	/**
	 * Set whether resolve changes should be written.
	 *
	 * @param writeOnChanges If <code>true</code>, then a change to the current
	 *            <code>-runbundles</code> value will be written to the output
	 *            bndrun file. The default is <code>false</code>.
	 */
	public void setWriteOnChanges(boolean writeOnChanges) {
		this.writeOnChanges = writeOnChanges;
	}

	/**
	 * Create a Resolve task.
	 */
	public Resolve() {
		super();
		outputBndrun = getProject().getObjects()
			.fileProperty()
			.convention(getBndrun());
	}

	/**
	 * Create the Bndrun object.
	 *
	 * @param workspace The workspace for the Bndrun.
	 * @param bndrunFile The bndrun file for the Bndrun.
	 * @return The Bndrun object.
	 * @throws Exception If the create action has an exception.
	 */
	@Override
	protected biz.aQute.resolve.Bndrun createRun(Workspace workspace, File bndrunFile) throws Exception {
		File outputBndrunFile = unwrap(getOutputBndrun());
		if (!Objects.equals(outputBndrunFile, bndrunFile)) {
			try (Writer writer = IO.writer(outputBndrunFile)) {
				UTF8Properties props = new UTF8Properties();
				props.setProperty(Constants.INCLUDE, String.format("\"%s\"", IO.absolutePath(bndrunFile)));
				props.store(writer, null);
			}
			bndrunFile = outputBndrunFile;
		}
		return biz.aQute.resolve.Bndrun.createBndrun(workspace, bndrunFile);
	}

	/**
	 * Resolve the Bndrun object.
	 *
	 * @param run The Run object.
	 * @throws Exception If the worker action has an exception.
	 */
	@Override
	protected void worker(biz.aQute.resolve.Bndrun run) throws Exception {
		getLogger().info("Resolving runbundles required for {}", run.getPropertiesFile());
		getLogger().debug("Run properties: {}", run.getProperties());
		try {
			String result = run.resolve(isFailOnChanges(), isWriteOnChanges());
			getLogger().info("{}: {}", Constants.RUNBUNDLES, result);
		} catch (ResolutionException e) {
			getLogger().error(ResolveProcess.format(e, isReportOptional()));
			throw new GradleException(String.format("%s resolution exception", run.getPropertiesFile()), e);
		} finally {
			logReport(run, getLogger());
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException(String.format("%s resolution failure", run.getPropertiesFile()));
		}
	}
}
