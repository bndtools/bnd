package aQute.jpm.platform;

import java.io.*;

import aQute.lib.io.*;

class Linux extends Platform {

	@Override public File getGlobal() {
		return new File("/var/jpm");
	}

	@Override public File getLocal() {
		File home = new File( System.getProperty("user.home"));
		return new File( home, ".jpm");
	}
	
	@Override public void link(String name, File file) throws IOException {
		String path = "/usr/local/bin/"+name;
		File link = new File( path);
		IO.copy(getClass().getResourceAsStream("mac.sh"), link);
		Runtime.getRuntime().exec("chmod a+x " + path);
	}
	@Override public void unlink(String name) throws IOException {
		String path = "/usr/local/bin/"+name;
		File link = new File( path);
		link.delete();
	}

	@Override public void shell(String initial) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override public String getName() { return "Linux"; }
}
