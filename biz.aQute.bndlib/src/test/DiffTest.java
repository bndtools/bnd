package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.service.diff.*;
import aQute.lib.osgi.*;

public class DiffTest extends TestCase {

	interface A extends Comparable, Serializable {

	}

	public void testSimple() throws Exception {

		Jar newer = new Jar(new File("jar/osgi.core-4.3.0.jar"));
		Jar older = new Jar(new File("jar/osgi.core.jar")); // 4.2
		DiffPluginImpl differ = new DiffPluginImpl();
		Diff diff = differ.diff(newer, older);

		show(diff, 0);		
	}

	void show(Diff diff, int indent) {
		if (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() == Delta.IGNORED)
			return;

		 for (int i = 0; i < indent; i++)
			 System.out.print("   ");

		System.out.println(diff.toString());

		if (diff.getDelta().isStructural())
			return;

		for (Diff c : diff.getChildren()) {
			show(c, indent + 1);
		}
	}

}
