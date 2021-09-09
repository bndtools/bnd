package aQute.bnd.gradle;

import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;

import aQute.bnd.repository.fileset.FileSetRepository;

/**
 * Task convention to make a FileSetRepository from a bundles property.
 *
 * @deprecated Function moved into AbstractBndrun type.
 */

@Deprecated
public class FileSetRepositoryConvention {
	private final AbstractBndrun<?, ?> bndrunTask;

	/**
	 * Create a FileSetRepositoryConvention for the specified AbstractBndrun
	 * task.
	 *
	 * @param task The AbstractBndrun task.
	 */
	public FileSetRepositoryConvention(Task task) {
		this.bndrunTask = (AbstractBndrun<?, ?>) task;
	}

	/**
	 * The bundles to be added to a FileSetRepository for non-Bnd Workspace
	 * builds.
	 * <p>
	 * This must not be used for Bnd Workspace builds.
	 *
	 * @return The bundles to be added to a FileSetRepository for non-Bnd
	 *         Workspace builds.
	 */
	public ConfigurableFileCollection getBundles() {
		return bndrunTask.getBundles();
	}

	/**
	 * Add files to use when locating bundles.
	 *
	 * @param paths The arguments will be handled using
	 *            ConfigurableFileCollection.from().
	 * @return The bundles to be added to a FileSetRepository for non-Bnd
	 *         Workspace builds.
	 */
	public ConfigurableFileCollection bundles(Object... paths) {
		return bndrunTask.bundles(paths);
	}

	/**
	 * Set the files to use when locating bundles.
	 * <p>
	 * The argument will be handled using ConfigurableFileCollection.from().
	 *
	 * @param path The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setBundles(Object path) {
		bndrunTask.setBundles(path);
	}

	/**
	 * Return a FileSetRepository with the specified name.
	 *
	 * @param name The name of the repository.
	 * @return A FileSetRepository with the specified name.
	 * @throws Exception An exception that occured when creating the repository.
	 */
	public FileSetRepository getFileSetRepository(String name) throws Exception {
		return new FileSetRepository(name, bndrunTask.getBundles()
			.getFiles());
	}
}
