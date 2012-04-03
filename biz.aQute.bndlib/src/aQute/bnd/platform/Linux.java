package aQute.bnd.platform;

import java.io.*;

class Linux extends Platform {

	@Override public File getGlobal() {
		return new File("/var/jpm");
	}

	@Override public File getLocal() {
		File home = new File( System.getProperty("user.home"));
		return new File( home, ".jpm");
	}
}
