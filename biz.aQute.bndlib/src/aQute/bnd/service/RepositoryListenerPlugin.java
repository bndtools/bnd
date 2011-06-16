package aQute.bnd.service;

import java.io.File;

import aQute.lib.osgi.Jar;

public interface RepositoryListenerPlugin {
	
	/**
	 * Called when a bundle is added to a repository.
	 * @param repository
	 * @param jar
	 * @param file
	 */
	void bundleAdded(RepositoryPlugin repository, Jar jar, File file);
}
