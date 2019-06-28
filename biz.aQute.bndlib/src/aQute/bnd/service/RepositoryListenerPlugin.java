package aQute.bnd.service;

import java.io.File;

import org.osgi.annotation.versioning.ConsumerType;

import aQute.bnd.osgi.Jar;

@ConsumerType
public interface RepositoryListenerPlugin {

	/**
	 * Called when a bundle is added to a repository.
	 *
	 * @param repository
	 * @param jar
	 * @param file
	 */
	void bundleAdded(RepositoryPlugin repository, Jar jar, File file);

	/**
	 * Called when a bundle removed from a repository.
	 *
	 * @param repository
	 * @param jar
	 * @param file
	 */
	void bundleRemoved(RepositoryPlugin repository, Jar jar, File file);

	/**
	 * Called when a large or unknown set of changes have occurred in the
	 * repository.
	 *
	 * @param repository
	 */
	void repositoryRefreshed(RepositoryPlugin repository);

	/**
	 * Called when a large or unknown set of changes have occurred, or may have
	 * occurred, in any or all repositories.
	 */
	void repositoriesRefreshed();

}
