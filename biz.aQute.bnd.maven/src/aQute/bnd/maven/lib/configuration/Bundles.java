package aQute.bnd.maven.lib.configuration;

import java.io.File;

public class Bundles extends FileTree {
	public Bundles() {}

	/**
	 * Add a bundle.
	 *
	 * @param bundle A bundle. A relative path is relative to the project base
	 *            directory.
	 */
	public void setBundle(File bundle) {
		addFile(bundle);
	}
}
