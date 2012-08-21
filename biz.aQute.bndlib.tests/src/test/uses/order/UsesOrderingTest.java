package test.uses.order;

import java.io.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.diff.*;

public class UsesOrderingTest extends TestCase {

	static DiffPluginImpl	differ	= new DiffPluginImpl();

	public static void testInheritance() throws Exception {
		Builder builder = new Builder();
		builder.addClasspath(new File("bin"));
		builder.setProperty(Constants.EXPORT_PACKAGE, "test.diff;uses:=d,c,a,b");
		Jar a = builder.build();

		String exa = (String) a.getManifest().getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
		
		builder = new Builder();
		builder.addClasspath(new File("bin"));
		builder.setProperty(Constants.EXPORT_PACKAGE, "test.diff;uses:=d,b,a,c");
		Jar b = builder.build();

		String exb = (String) b.getManifest().getMainAttributes().getValue(Constants.EXPORT_PACKAGE);

		Tree newer = differ.tree(b);
		Tree older = differ.tree(a);

		Diff diff = newer.diff(older);

		show(diff, 0);
		
		assertEquals(Delta.UNCHANGED, diff.getDelta());
	}

	static void show(Diff diff, int indent) {
		// if (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() ==
		// Delta.IGNORED)
		// return;

		for (int i = 0; i < indent; i++)
			System.err.print("   ");

		System.err.println(diff.toString());

		// if (diff.getDelta().isStructural())
		// return;

		for (Diff c : diff.getChildren()) {
			show(c, indent + 1);
		}
	}
}
