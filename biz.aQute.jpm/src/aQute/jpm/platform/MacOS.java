package aQute.jpm.platform;

import java.io.*;

import aQute.lib.io.*;

class MacOS extends Platform {
	File home = new File( System.getProperty("user.home"));

	@Override public File getGlobal() {
		return new File("/Library/Java/JavaPackageManager");
	}

	@Override public File getLocal() {
		return new File( home, "Library/JavaPackageManager");
	}

	@Override public void link(String name, File file) throws IOException {
		String path = "/usr/local/bin/"+name;
		File link = new File( path).getAbsoluteFile();
		link.getParentFile().mkdirs();
		
		String sh = IO.collect( getClass().getResourceAsStream("mac.sh"));
		sh = sh.replaceAll("%file%", file.getAbsolutePath());
		IO.store(sh, link);
		Runtime.getRuntime().exec("chmod a+x " + path);
	}

	@Override public void unlink(String name) throws IOException {
		String path = "/usr/local/bin/"+name;
		File link = new File( path);
		link.delete();
	}

	@Override public void shell(String initial) throws IOException {
		Runtime.getRuntime().exec("open -n /Applications/Utilities/Terminal.app");
	}
	@Override public String getName() { return "MacOS"; }
	
}
