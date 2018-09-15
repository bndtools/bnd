package test.component;

import java.io.File;

import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class ComponentOrderingTest extends TestCase {

	static DiffPluginImpl differ = new DiffPluginImpl();

	public static void testOrdering() throws Exception {
		Builder builder = new Builder();
		builder.addClasspath(new File("bin_test"));
		builder.setProperty("Service-Component", "OSGI-INF/a.xml,OSGI-INF/b.xml,OSGI-INF/c.xml,OSGI-INF/d.xml");
		Jar a = builder.build();

		String exa = a.getManifest()
			.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE);

		builder = new Builder();
		builder.addClasspath(new File("bin_test"));
		builder.setProperty("Service-Component", "OSGI-INF/d.xml,OSGI-INF/b.xml,OSGI-INF/a.xml,OSGI-INF/c.xml");
		Jar b = builder.build();

		String exb = b.getManifest()
			.getMainAttributes()
			.getValue("Service-Component");

		Tree newer = differ.tree(b);
		Tree older = differ.tree(a);

		Diff diff = newer.diff(older);

		assertEquals(Delta.UNCHANGED, diff.getDelta());
	}
}
