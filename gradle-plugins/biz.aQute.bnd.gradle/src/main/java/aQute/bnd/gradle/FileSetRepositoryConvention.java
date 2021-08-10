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

	public FileSetRepositoryConvention(Task task) {
		this.bndrunTask = (AbstractBndrun<?, ?>) task;
	}

	public ConfigurableFileCollection getBundles() {
		return bndrunTask.getBundles();
	}

	public ConfigurableFileCollection bundles(Object... paths) {
		return bndrunTask.bundles(paths);
	}

	public void setBundles(Object path) {
		bndrunTask.setBundles(path);
	}

	public FileSetRepository getFileSetRepository(String name) throws Exception {
		return new FileSetRepository(name, bndrunTask.getBundles()
			.getFiles());
	}
}
