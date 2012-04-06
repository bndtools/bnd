package aQute.jpm.platform;

import static aQute.lib.io.IO.*;

import java.io.*;

import aQute.lib.collections.*;
import aQute.lib.io.*;
import aQute.libg.sed.*;

public abstract class Unix extends Platform {

	public String getBinPrefix() {
		return "/usr/local/bin/";
	}

	@Override public File getGlobal() {
		return new File("/var/jpm");
	}

	@Override public File getLocal() {
		File home = new File(System.getProperty("user.home"));
		return new File(home, ".jpm");
	}

	@Override public void createCommand(String name, File file) throws Exception {
		String path = getBinPrefix() + name;
		File link = new File(path).getAbsoluteFile();
		link.getParentFile().mkdirs();

		String sh = IO.collect(getClass().getResourceAsStream("run-unix.sh"));
		sh = sh.replaceAll("%file%", file.getAbsolutePath());
		IO.store(sh, link);
		run("chmod a+x " + path);
	}

	@Override public void deleteCommand(String name) throws Exception {
		String path = getBinPrefix() + name;
		File link = new File(path);
		link.delete();
	}

	@Override public void createService(File base, File[] path, String main, String... args) throws Exception {
		File wdir = new File(base, "work");
		wdir.mkdir();
		File start = new File(base, "start");
		copy(getClass().getResourceAsStream("service-unix.sh"), start);

		Sed sed = new Sed(start);
		sed.setBackup(false);
		sed.replace("%service%", base.getName());
		sed.replace("%path%", new ExtList<File>(path).join(":"));
		sed.replace("%wdir%", wdir.getAbsolutePath());
		sed.replace("%vmargs%", "");
		sed.replace("%main%", main);
		sed.replace("%args%", new ExtList<String>(args).join(" "));
		sed.replace("%file%", path[0].getAbsolutePath());
		sed.doIt();
		run("chmod a+x " + start);		
	}

}
