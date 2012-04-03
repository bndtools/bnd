package aQute.bnd.platform;

import java.io.*;

class MacOS extends Platform {

	@Override public File getGlobal() {
		return new File("/Library/Java/JavaPackageManager");
	}

	@Override public File getLocal() {
		File home = new File( System.getProperty("user.home"));
		return new File( home, "Library/JavaPackageManager");
	}

}
