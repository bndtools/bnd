/**
 * Task convention to make a FileSetRepository from
 * a bundles property.
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bundles - This is the collection of files to use for locating
 * bundles during the bndrun execution. The default is
 * "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files"</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy

import aQute.bnd.repository.fileset.FileSetRepository

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles


class FileSetRepositoryConvention {
	/**
	 * The bundles for the repository.
	 */
	@InputFiles
	final ConfigurableFileCollection bundles

	/**
	 * Create a FileSetRepositoryConvention.
	 *
	 */
	FileSetRepositoryConvention(Task task) {
		Project project = task.getProject()
		bundles = project.getObjects().fileCollection()
		bundles(project.sourceSets.main.getRuntimeClasspath())
		bundles(project.getConfigurations().archives.getArtifacts().getFiles())
		// need to programmatically add to inputs since @InputFiles in a convention is not processed
		task.getInputs().files(getBundles()).withPropertyName("bundles")
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
	 * Return a FileSetRepository using the bundles.
	 */
	FileSetRepository getFileSetRepository(String name) {
		return new FileSetRepository(name, getBundles().getFiles())
	}
}
