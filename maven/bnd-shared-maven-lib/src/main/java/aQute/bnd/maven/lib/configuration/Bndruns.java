package aQute.bnd.maven.lib.configuration;

import java.io.File;

public class Bndruns extends FileTree {
	public Bndruns() {}

	public void setBndrun(File bndrun) {
		addFile(bndrun);
	}
}
