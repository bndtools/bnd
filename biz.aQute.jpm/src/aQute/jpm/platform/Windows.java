package aQute.jpm.platform;

import java.io.*;

import aQute.lib.io.*;

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
	
	@Override public void link(String name, File file) throws IOException {
		String path = "/usr/bin/"+name + ".bat";
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
		Runtime.getRuntime().exec("command.com");
	}

	@Override public String getName() { return "Windows"; }

}
