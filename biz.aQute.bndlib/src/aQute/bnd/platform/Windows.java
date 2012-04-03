package aQute.bnd.platform;

import java.io.*;

class Windows extends Platform {
	@Override public File getGlobal() {
		String global = System.getenv("APPDATA");
		if ( global == null) {
			
		}
		return new File("/Library/Java/JavaPackageManager");
	}

	@Override public File getLocal() {
		String s = System.getenv("APPDATA");
		if ( s == null) {
			s = System.getenv("user.home") + "\\Application Data\\JavaPackageManager";
		}
			
		return new File(s);
	}


}
