package aQute.bnd.gradle

import aQute.bnd.repository.fileset.FileSetRepository

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection

/**
 * Task convention to make a FileSetRepository from
 * a bundles property.
 * 
 * @deprecated Function moved into Bndrun task type.
 */

@Deprecated
class FileSetRepositoryConvention {
	private final Bndrun bndrunTask

	FileSetRepositoryConvention(Task task) {
		this.bndrunTask = task
	}

	public ConfigurableFileCollection getBundles() {
		return bndrunTask.getBundles()
	}

	public ConfigurableFileCollection bundles(Object... paths) {
		return bndrunTask.bundles(paths)
	}

	public void setBundles(Object path) {
		bndrunTask.setBundles(path)
	}

	FileSetRepository getFileSetRepository(String name) {
		return new FileSetRepository(name, bndrunTask.getBundles().getFiles())
	}
}
