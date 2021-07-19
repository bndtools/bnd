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
	private final BundleTaskExtension				extension;
	private final org.gradle.api.tasks.bundling.Jar	task;
	private boolean									classpathModified;

	public BundleTaskConvention(BundleTaskExtension extension, org.gradle.api.tasks.bundling.Jar task) {
		this.extension = extension;
		this.task = task;
		classpathModified = false;
	}

	public BundleTaskConvention(org.gradle.api.tasks.bundling.Jar task) {
		this(getBundleTaskExtension(task), task);
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

	public RegularFileProperty getBndfile() {
		return extension.getBndfile();
	}

	public void setBndfile(String file) {
		extension.getBndfile()
			.value(task.getProject()
				.getLayout()
				.getProjectDirectory()
				.file(file));
	}

	public void setBndfile(Object file) {
		extension.getBndfile()
			.set(task.getProject()
				.file(file));
	}

	public Provider<String> getBnd() {
		return extension.getBnd();
	}

	public void setBnd(CharSequence line) {
		extension.setBnd(line);
	}

	public void setBnd(Provider<? extends CharSequence> lines) {
		extension.setBnd(lines);
	}

	public void bnd(CharSequence... lines) {
		extension.bnd(lines);
	}

	public void bnd(Provider<? extends CharSequence> lines) {
		extension.bnd(lines);
	}

	public void setBnd(Map<String, ?> map) {
		extension.setBnd(map);
	}

	public void bnd(Map<String, ?> map) {
		extension.bnd(map);
	}

	public ConfigurableFileCollection getClasspath() {
		return extension.getClasspath();
	}

	public ConfigurableFileCollection classpath(Object... paths) {
		classpathModified = true;
		return extension.classpath(paths);
	}

	public void setClasspath(Object path) {
		classpathModified = true;
		extension.setClasspath(path);
	}

	public void setSourceSet(SourceSet sourceSet) {
		extension.setSourceSet(sourceSet);
		if (!classpathModified) {
			extension.setClasspath(sourceSet.getCompileClasspath());
		}
	}

	public void buildBundle() throws Exception {
		extension.buildBundle();
	}
}
