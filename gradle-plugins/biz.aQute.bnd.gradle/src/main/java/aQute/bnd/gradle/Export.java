package aQute.bnd.gradle;

import static aQute.bnd.exporter.executable.ExecutableJarExporter.EXECUTABLE_JAR;
import static aQute.bnd.exporter.runbundles.RunbundlesExporter.RUNBUNDLES;
import static aQute.bnd.gradle.BndUtils.distDirectory;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.model.ReplacedBy;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Export task type for Gradle.
 * <p>
 * This task type can be used to export a bndrun file.
 * <p>
 * Here is examples of using the Export task type:
 *
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
 * <p>
 * Properties:
 * <ul>
 * <li>bndrun - This is the bndrun file to be exported. This property must be
 * set.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd
 * Workspace builds. The default is "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files". This must not be used for Bnd
 * Workspace builds.</li>
 * <li>ignoreFailures - If true the task will not fail if the export fails. The
 * default is false.</li>
 * <li>workingDirectory - This is the directory for the export operation. The
 * default for workingDirectory is temporaryDir.</li>
 * <li>properties - Properties that are available for evaluation of the bnd
 * instructions for non-Bnd Workspace builds. The default is the properties of
 * the task and project objects. This must not be used for Bnd Workspace
 * builds.</li>
 * <li>destinationDirectory - This is the directory for the output. The default
 * for destinationDirectory is project.base.distsDirectory.dir("executable") if
 * the exporter is "bnd.executablejar",
 * project.base.distsDirectory.dir("runbundles"/bndrun) if the exporter is
 * "bnd.runbundles", and project.base.distsDirectory.dir(task.name) for all
 * other exporters.</li>
 * <li>exporter - The name of the exporter plugin to use. Bnd has two built-in
 * exporter plugins. "bnd.executablejar" exports an executable jar and
 * "bnd.runbundles" exports the -runbundles files. The default is
 * "bnd.executablejar".</li>
 * </ul>
 */
public class Export extends AbstractBndrun {
	private boolean					bundlesOnly	= false;
	private final DirectoryProperty	destinationDirectory;
	private final Property<String>	exporter;

	/**
	 * @return bundlesOnly
	 * @deprecated Replaced by exporter.
	 */
	@ReplacedBy("exporter")
	@Deprecated
	public boolean isBundlesOnly() {
		return bundlesOnly;
	}

	/**
	 * @return bundlesOnly
	 * @deprecated Replaced by exporter.
	 */
	@Internal
	@Deprecated
	public boolean getBundlesOnly() {
		return isBundlesOnly();
	}

	/**
	 * @param bundlesOnly Replaced by exporter.
	 * @deprecated Replaced by exporter.
	 */
	@Deprecated
	public void setBundlesOnly(boolean bundlesOnly) {
		this.bundlesOnly = bundlesOnly;
	}

	/**
	 * The destination directory for the export.
	 * <p>
	 * The default for destinationDirectory is
	 * project.base.distsDirectory.dir("executable") if the exporter is
	 * "bnd.executablejar", project.base.distsDirectory.dir("runbundles"/bndrun)
	 * if the exporter is "bnd.runbundles", and
	 * project.base.distsDirectory.dir(task.name) for all other exporters.
	 *
	 * @return The destination directory for the export.
	 */
	@OutputDirectory
	public DirectoryProperty getDestinationDirectory() {
		return destinationDirectory;
	}

	/**
	 * The name of the exporter for this task.
	 * <p>
	 * Bnd has two built-in exporter plugins. "bnd.executablejar" exports an
	 * executable jar and "bnd.runbundles" exports the -runbundles files. The
	 * default is "bnd.executablejar" unless bundlesOnly is false when the
	 * default is "bnd.runbundles".
	 *
	 * @return The name of the exporter for this task.
	 */
	@Input
	public Property<String> getExporter() {
		return exporter;
	}

	/**
	 * Create a Export task.
	 */
	public Export() {
		super();
		org.gradle.api.Project project = getProject();
		ObjectFactory objects = project.getObjects();
		exporter = objects.property(String.class)
			.convention(project.provider(() -> getBundlesOnly() ? RUNBUNDLES : EXECUTABLE_JAR));
		Provider<Directory> distsDirectory = distDirectory(project);
		destinationDirectory = objects.directoryProperty()
			.convention(distsDirectory.flatMap(distsDir -> {
				return distsDir.dir(getExporter().map(exporterName -> {
					switch (exporterName) {
						case EXECUTABLE_JAR :
							return "executable";
						case RUNBUNDLES :
							File bndrunFile = unwrapFile(getBndrun());
							String[] parts = Strings.extension(bndrunFile.getName());
							return String.format("runbundles/%s", (parts != null) ? parts[0] : bndrunFile.getName());
						default :
							return exporterName;
					}
				}));
			}));
	}

	/**
	 * Export the Project object.
	 *
	 * @param run The Project object.
	 * @throws Exception If the worker action has an exception.
	 */
	@Override
	protected void worker(Project run) throws Exception {
		String exporterName = unwrap(getExporter());
		File destinationDirFile = unwrapFile(getDestinationDirectory());
		getLogger().info("Exporting {} to {} with exporter {}", run.getPropertiesFile(), destinationDirFile,
			exporterName);
		getLogger().debug("Run properties: {}", run.getProperties());
		try {
			Map.Entry<String, Resource> export = run.export(exporterName, Collections.emptyMap());
			if (Objects.nonNull(export)) {
				if (Objects.equals(exporterName, RUNBUNDLES)) {
					try (JarResource jr = (JarResource) export.getValue()) {
						jr.getJar()
							.writeFolder(destinationDirFile);
					}
				} else {
					try (Resource r = export.getValue()) {
						File exported = IO.getBasedFile(destinationDirFile, export.getKey());
						r.write(exported);
						exported.setLastModified(r.lastModified());
					}
				}
			}
		} finally {
			logReport(run, getLogger());
		}
		if (!isIgnoreFailures() && !run.isOk()) {
			throw new GradleException(String.format("%s export failure", run.getPropertiesFile()));
		}
	}
}
