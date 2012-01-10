package aQute.bnd.main;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.configurable.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;

public class SlurpCommand {
	bnd	bnd;

	interface slurp extends IGetOpt {
		@Config(description = "Output file") String output();
	}

	public static void diff(bnd bnd, String[] args, int first, PrintStream out) throws Exception {

	}

	public static void slurp(bnd bnd, String[] args, int i, PrintStream out) throws Exception {
		slurp cmd = GetOpt.getopt(args, i, slurp.class);
		if (cmd._().size() != 1) {
			bnd.error("Requires 1 file name");
			return;
		}

		File f = bnd.getFile(cmd._().get(0));
		if (!f.isFile()) {
			bnd.error("No such file %s", f);
			return;
		}

		Jar jar = new Jar(f);
		List<Closeable> closed = new ArrayList<Closeable>();
		try {

			Manifest m = jar.getManifest();
			String paths = m.getMainAttributes().getValue("Class-Path");
			if (paths == null) {
				bnd.error("No Class-Path header in %s", f);
				return;
			}

			String p[] = paths.split("\\s+");
			File base = f.getParentFile();

			for (String path : p) {
				File sub = IO.getFile(base, path);
				Jar jsub = new Jar(sub);
				closed.add(jsub);
				jar.addAll(jsub);
			}

			String o = cmd.output();
			if (o == null) {
				o = f.getName() + ".slurp";
			}
			m.getMainAttributes().remove( new Attributes.Name("Class-Path"));
			jar.setManifest(m);
			jar.write(bnd.getFile(o));
		} finally {
			for ( Closeable c : closed) {
				c.close();
			}
			jar.close();
		}
	}
}
