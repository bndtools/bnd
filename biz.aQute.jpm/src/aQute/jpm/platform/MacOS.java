package aQute.jpm.platform;

import java.io.*;

class MacOS extends Unix {
	File home = new File( System.getProperty("user.home"));

	@Override public File getGlobal() {
		return new File("/Library/Java/PackageManager").getAbsoluteFile();
	}

	@Override public File getLocal() {
		return new File( home, "Library/PackageManager").getAbsoluteFile();
	}



	@Override public void shell(String initial) throws Exception {
		run("open -n /Applications/Utilities/Terminal.app");
	}
	@Override public String getName() { return "MacOS"; }
	
	@Override public void uninstall() {
		
	}


}
