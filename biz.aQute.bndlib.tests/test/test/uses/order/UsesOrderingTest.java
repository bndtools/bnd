package test.uses.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;

@SuppressWarnings("resource")

public class UsesOrderingTest {


	@Test
	public void testOrdering() throws Exception {
		DiffPluginImpl differ = new DiffPluginImpl();
		Builder builder = new Builder();
		builder.addClasspath(new File("bin_test"));
		builder.setProperty(Constants.EXPORT_PACKAGE, "test.diff;uses:=\"d,c,a,b\"");
		Jar a = builder.build();

		String exa = a.getManifest()
			.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE);

		builder = new Builder();
		builder.addClasspath(new File("bin_test"));
		builder.setProperty(Constants.EXPORT_PACKAGE, "test.diff;uses:=\"d,b,a,c\"");
		Jar b = builder.build();

		String exb = b.getManifest()
			.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE);

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
