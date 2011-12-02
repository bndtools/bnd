package test;

import java.io.*;

import junit.framework.*;
import aQute.lib.jardiff.*;
import aQute.lib.jardiff.Diff.*;
import aQute.lib.jardiff.java.*;
import aQute.lib.osgi.*;

public class DiffTest extends TestCase {

	public void testSimple() throws Exception {

		Jar newer = new Jar(new File("jar/osgi.core-4.3.0.jar"));
		Jar older = new Jar(new File("jar/osgi.core.jar")); // 4.2

		JarDiff jd = new JarDiff(newer, older);
		jd.compare();
		show(jd, 0);

	}

	void show(Diff diff, int indent) {
		if (diff.getDelta() == Delta.UNCHANGED)
			return;

		for (int i = 0; i < indent; i++)
			System.out.print(" ");

		VersionDiff vd = diff.adapt(VersionDiff.class);
		if (vd != null) {
			System.out.println(diff.getDelta() + " " + diff.getName() + " " + vd.getOldVersion()
					+ " -> " + vd.getNewVersion());
		} else
			System.out.println(diff.getDelta() + " " + diff.toString());
		if (diff.getDelta() == Delta.ADDED || diff.getDelta() == Delta.REMOVED)
			return;

		for (Diff c : diff.getContained()) {
			show(c, indent + 1);
		}
	}
}
