package aQute.bnd.gradle

import static aQute.bnd.exporter.executable.ExecutableJarExporter.EXECUTABLE_JAR
import static aQute.bnd.exporter.runbundles.RunbundlesExporter.RUNBUNDLES
import static aQute.bnd.gradle.BndUtils.isGradleCompatible
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap

import aQute.lib.io.IO

import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory

/**
 * Export task type for Gradle.
 *
 * <p>
 * This task type can be used to export a bndrun file.
 *
 * <p>
 * Here is examples of using the Export task type:
 * <pre>
 * import aQute.bnd.gradle.Export
 * tasks.register("exportExecutable", Export) {
 *   bndrun = file("my.bndrun")
 *   exporter = "bnd.executablejar"
 * }
 * tasks.register("exportRunbundles", Export) {
 *   bndrun = file("my.bndrun")
 *   exporter = "bnd.runbundles"
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the task will not fail if the export
 * fails. The default is false.</li>
 * <li>exporter - The name of the exporter plugin to use.
 * Bnd has two built-in exporter plugins. "bnd.executablejar"
 * exports an executable jar and "bnd.runbundles" exports the
 * -runbundles files. The default is "bnd.executablejar".</li>
 * <li>bndrun - This is the bndrun file to be exported.
 * This property must be set.</li>
 * <li>destinationDirectory - This is the directory for the output.
 * The default for destinationDirectory is project.base.distsDirectory.dir("executable")
 * if the exporter is "bnd.executablejar", project.base.distsDirectory.dir("runbundles"/bndrun)
 * if the exporter is "bnd.runbundles", and project.base.distsDirectory.dir(task.name)
 * for all other exporters.</li>
 * <li>workingDirectory - This is the directory for the export operation.
 * The default for workingDirectory is temporaryDir.</li>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files".</li>
 * </ul>
 */
public class Export extends Bndrun {
	/**
	 * This property is replaced by exporter.
	 * This property is only used when the exporter
	 * property is not specified.
	 *
	 * <p>
	 * If <code>true</code>, then the exporter defaults to
	 * "bnd.runbundles". Otherwise the exporter defaults to
	 * "bnd.executablejar". The default is <code>false</code>.
	 */
	@ReplacedBy("exporter")
	@Deprecated
	boolean bundlesOnly = false

	/**
	 * The name of the exporter for this task.
	 *
	 * <p>
	 * Bnd has two built-in exporter plugins. "bnd.executablejar"
	 * exports an executable jar and "bnd.runbundles" exports the
	 * -runbundles files. The default is "bnd.executablejar" unless
	 * bundlesOnly is false when the default is "bnd.runbundles".
	 */
	@Input
	final Property<String> exporter

	/**
	 * The destination directory for the export.
	 *
	 * <p>
	 * The default for destinationDirectory is project.base.distsDirectory.dir("executable")
	 * if the exporter is "bnd.executablejar", project.base.distsDirectory.dir("runbundles"/bndrun)
	 * if the exporter is "bnd.runbundles", and project.base.distsDirectory.dir(task.name)
	 * for all other exporters.
	 */
	@OutputDirectory
	final DirectoryProperty destinationDirectory

	/**
	 * Create a Export task.
	 *
	 */
	public Export() {
		super()
		ObjectFactory objects = getProject().getObjects()
		exporter = objects.property(String.class).convention(getProject().provider(() -> getBundlesOnly() ? RUNBUNDLES : EXECUTABLE_JAR))
		Provider<Directory> distsDirectory = isGradleCompatible("7.1") ? getProject().base.getDistsDirectory()
		: getProject().distsDirectory
		destinationDirectory = objects.directoryProperty().convention(distsDirectory.flatMap(distsDir -> {
			return distsDir.dir(getExporter().map(exporterName -> {
				switch(exporterName) {
					case EXECUTABLE_JAR:
					return "executable"
					case RUNBUNDLES:
					File bndrunFile = unwrap(getBndrun())
					String bndrunName = bndrunFile.getName() - ".bndrun"
					return "runbundles/${bndrunName}"
					default:
					return exporterName
				}
			}))
		}))
	}

	@Deprecated
	@ReplacedBy("destinationDirectory")
	public File getDestinationDir() {
		return unwrap(getDestinationDirectory())
	}

	@Deprecated
	public void setDestinationDir(Object dir) {
		getDestinationDirectory().set(getProject().file(dir))
	}

	/**
	 * Export the Run object.
	 */
	@Override
	protected void worker(var run) {
		String exporterName = unwrap(getExporter())
		File destinationDirFile = unwrap(getDestinationDirectory())
		getLogger().info("Exporting {} to {} with exporter {}", run.getPropertiesFile(), destinationDirFile, exporterName)
		getLogger().debug("Run properties: {}", run.getProperties())
		try {
			Map.Entry<String, ?> export = run.export(exporterName, Collections.emptyMap())
			if (Objects.nonNull(export)) {
				if (Objects.equals(exporterName, RUNBUNDLES)) {
					try (var jr = export.getValue()) {
						jr.getJar().writeFolder(destinationDirFile)
					}
				} else {
					try (var r = export.getValue()) {
						File exported = IO.getBasedFile(destinationDirFile, export.getKey())
						try (OutputStream out = IO.outputStream(exported)) {
							r.write(out)
						}
						exported.setLastModified(r.lastModified())
					}
				}
			}
		} finally {
			logReport(run, getLogger())
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException("${run.getPropertiesFile()} export failure")
		}
	}
}
