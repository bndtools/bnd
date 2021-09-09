package aQute.bnd.gradle;

import java.util.Map;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

/**
 * BundleTaskConvention for Gradle.
 *
 * @deprecated Replaced by BundleTaskExtension.
 */
@Deprecated
public class BundleTaskConvention {
	private final BundleTaskExtension	extension;
	private boolean						classpathModified;

	/**
	 * Create a BundleTaskConvention for the specified BundleTaskExtension.
	 *
	 * @param extension The BundleTaskExtension for this convention.
	 */
	public BundleTaskConvention(BundleTaskExtension extension) {
		this.extension = extension;
		classpathModified = false;
	}

	/**
	 * Create a BundleTaskConvention for the specified Jar task.
	 *
	 * @param task The Jar task for this convention.
	 */
	public BundleTaskConvention(org.gradle.api.tasks.bundling.Jar task) {
		this(getBundleTaskExtension(task));
	}

	private static BundleTaskExtension getBundleTaskExtension(org.gradle.api.tasks.bundling.Jar task) {
		BundleTaskExtension extension = task.getExtensions()
			.findByType(BundleTaskExtension.class);
		if (extension != null) {
			return extension;
		}
		return task.getExtensions()
			.create(BundleTaskExtension.NAME, BundleTaskExtension.class, task);
	}

	/**
	 * The bndfile property.
	 * <p>
	 * A bnd file containing bnd instructions for this project.
	 *
	 * @return The property for the bndfile.
	 */
	public RegularFileProperty getBndfile() {
		return extension.getBndfile();
	}

	/**
	 * Set the bndfile property value.
	 *
	 * @param file The bndfile name.
	 */
	public void setBndfile(String file) {
		extension.getBndfile()
			.value(extension.getTask()
				.getProject()
				.getLayout()
				.getProjectDirectory()
				.file(file));
	}

	/**
	 * Set the bndfile property value.
	 *
	 * @param file The bndfile object.
	 */
	public void setBndfile(Object file) {
		extension.getBndfile()
			.set(extension.getTask()
				.getProject()
				.file(file));
	}

	/**
	 * The bnd property.
	 * <p>
	 * If the bndfile property points an existing file, this property is
	 * ignored. Otherwise, the bnd instructions in this property will be used.
	 *
	 * @return The property for the bnd instructions.
	 */
	public Provider<String> getBnd() {
		return extension.getBnd();
	}

	/**
	 * Set the bnd property from a multi-line string.
	 *
	 * @param line bnd instructions.
	 */
	public void setBnd(CharSequence line) {
		extension.setBnd(line);
	}

	/**
	 * Set the bnd property from a multi-line string using a {@link Provider}.
	 *
	 * @param lines A provider of bnd instructions.
	 */
	public void setBnd(Provider<? extends CharSequence> lines) {
		extension.setBnd(lines);
	}

	/**
	 * Add instructions to the bnd property from a list of multi-line strings.
	 *
	 * @param lines bnd instructions.
	 */
	public void bnd(CharSequence... lines) {
		extension.bnd(lines);
	}

	/**
	 * Add a multi-line string of instructions to the bnd property using a
	 * {@link Provider}.
	 *
	 * @param lines A provider bnd instructions.
	 */
	public void bnd(Provider<? extends CharSequence> lines) {
		extension.bnd(lines);
	}

	/**
	 * Set the bnd property from a map.
	 *
	 * @param map A map of bnd instructions.
	 */
	public void setBnd(Map<String, ?> map) {
		extension.setBnd(map);
	}

	/**
	 * Add instructions to the bnd property from a map.
	 *
	 * @param map A map of bnd instructions.
	 */
	public void bnd(Map<String, ?> map) {
		extension.bnd(map);
	}

	/**
	 * The classpath property.
	 * <p>
	 * The default value is sourceSets.main.compileClasspath.
	 *
	 * @return The property for the classpath.
	 */
	public ConfigurableFileCollection getClasspath() {
		return extension.getClasspath();
	}

	/**
	 * Add files to the classpath.
	 *
	 * @param paths The arguments will be handled using
	 *            ConfigurableFileCollection.from().
	 * @return The property for the classpath.
	 */
	public ConfigurableFileCollection classpath(Object... paths) {
		classpathModified = true;
		return extension.classpath(paths);
	}

	/**
	 * Set the classpath property.
	 *
	 * @param path The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setClasspath(Object path) {
		classpathModified = true;
		extension.setClasspath(path);
	}

	/**
	 * Set the sourceSet.
	 *
	 * @param sourceSet A sourceSet to use to find source code.
	 */
	public void setSourceSet(SourceSet sourceSet) {
		extension.setSourceSet(sourceSet);
		if (!classpathModified) {
			extension.setClasspath(sourceSet.getCompileClasspath());
		}
	}

	/**
	 * Execute the Action to build the bundle for the task.
	 *
	 * @throws Exception An exception that occurred during the bundle build.
	 */
	public void buildBundle() throws Exception {
		extension.buildAction()
			.execute(extension.getTask());
	}
}
