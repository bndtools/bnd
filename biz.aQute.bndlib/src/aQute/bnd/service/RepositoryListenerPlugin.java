package aQute.bnd.service;

import java.io.*;

import aQute.lib.osgi.*;

public interface RepositoryListenerPlugin {

	/**
	 * Called when a bundle is added to a repository.
	 * 
	 * @param repository
	 * @param jar
	 * @param file
	 */
	void bundleAdded(RepositoryPlugin repository, Jar jar, File file);
}
