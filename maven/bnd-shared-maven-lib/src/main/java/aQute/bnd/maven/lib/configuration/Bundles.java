package aQute.bnd.maven.lib.configuration;

import java.io.File;

public class Bundles extends FileTree {
	public Bundles() {}

	public void setBundle(File bundle) {
		addFile(bundle);
	}
}
