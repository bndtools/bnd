package aQute.bld.main;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import aQute.lib.io.*;
import aQute.libg.classloaders.*;
import aQute.libg.filerepo.*;
import aQute.libg.version.*;

public class Main {

	public static void main(String args[]) throws Exception {
		URLClassLoaderWrapper wrapper = new URLClassLoaderWrapper(Main.class.getClassLoader());
		
		File dir = new File("").getAbsoluteFile();
		File rover = dir;
		while (rover != null && rover.isDirectory()) {
			File bld = new File(rover, "cnf");
			if (!bld.exists())
				bld = new File(rover, "bnd");

			if (bld.isDirectory()) {
				// We need to get bndlib
				Properties p = new Properties();
				File pfile = new File(bld, "build.bnd");
				FileInputStream in = new FileInputStream(pfile);
				try {
					p.load(in);
				} finally {
					in.close();
				}

				File bndlib = null;
				String b = p.getProperty("-bndlib");
				if (b != null) {
					bndlib = IO.getFile(bld, b);
				} else {
					String r = p.getProperty("-repo", "repo");
					File repo = IO.getFile(bld, r);
					FileRepo fr = new FileRepo(repo);
					bndlib = fr.get("biz.aQute.bndlib", new VersionRange("0"), 1);
				}

				if (bndlib == null) {
					System.err.println("Cannot locate bndlib");
					return;
				}

				wrapper.addURL(bndlib.toURI().toURL());
				
				// we do not want to touch the class before we can load bndlib
				Class<?> bndrun = wrapper.loadClass("aQute.bld.main.Run");
				Object run = bndrun.newInstance();
				Method m = bndrun.getMethod("run", String[].class, File.class, File.class, URLClassLoaderWrapper.class);
				m.invoke(run, (Object[]) args, dir, bld, wrapper);
				return;
			} else if (bld.isFile()) {
				String link = IO.collect(bld);
				rover = IO.getFile(bld, link);
			} else
				rover = rover.getParentFile();
		}
		System.err.println("Cannot find cnf or bnd directory in parents");
	}


}
