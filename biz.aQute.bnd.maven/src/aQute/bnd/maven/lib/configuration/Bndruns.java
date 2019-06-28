package aQute.bnd.maven.lib.configuration;

import java.io.File;

public class Bndruns extends FileTree {
	public Bndruns() {}

	/**
	 * Add a bndrun file.
	 *
	 * @param bndrun A bndrun file. A relative path is relative to the project
	 *            base directory.
	 */
	public void setBndrun(File bndrun) {
		addFile(bndrun);
	}
}
