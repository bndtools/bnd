package test.diff;

import java.io.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.service.diff.*;
import aQute.lib.osgi.*;

public class DiffTest extends TestCase {
	DiffPluginImpl differ = new DiffPluginImpl();

	public class II {
		public void foo() {}
	}
	public class I extends II {
		
	}
	public void testInheritance() throws Exception {
		Builder b = new Builder();
		b.addClasspath( new File("bin"));
		b.setProperty(Constants.EXPORT_PACKAGE, "test");
		b.build();
		Diff diff = differ.diff(b, b);
		Diff p = diff.get("<api>").get("test");
		show(p,0);
		Diff c = p.get("test.DiffTest$I");
		assertNotNull(c.get("int hashCode()"));
		assertNotNull(c.get("void finalize()"));
		assertNotNull(c.get("void foo()"));
		b.close();
	}
	
	interface A extends Comparable, Serializable {

	}

	public void testSimple() throws Exception {

		Jar newer = new Jar(new File("jar/osgi.core-4.3.0.jar"));
		Jar older = new Jar(new File("jar/osgi.core.jar")); // 4.2
		Diff diff = differ.diff(newer, older);

		show(diff, 0);		
	}

	
	
	void show(Diff diff, int indent) {
		if (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() == Delta.IGNORED)
			return;

		 for (int i = 0; i < indent; i++)
			 System.out.print("   ");

		System.out.println(diff.toString());

		if (diff.getDelta() == Delta.ADDED || diff.getDelta() == Delta.REMOVED)
			return;

		for (Diff c : diff.getChildren()) {
			show(c, indent + 1);
		}
	}

}
