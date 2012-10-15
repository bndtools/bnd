package test.component;

import java.io.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.diff.*;

public class ComponentOrderingTest extends TestCase {

	static DiffPluginImpl	differ	= new DiffPluginImpl();

	public static void testOrdering() throws Exception {
		Builder builder = new Builder();
		builder.addClasspath(new File("bin"));
		builder.setProperty("Service-Component", "OSGI-INF/a.xml,OSGI-INF/b.xml,OSGI-INF/c.xml,OSGI-INF/d.xml");
		Jar a = builder.build();

		String exa = (String) a.getManifest().getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
		
		builder = new Builder();
		builder.addClasspath(new File("bin"));
		builder.setProperty("Service-Component", "OSGI-INF/d.xml,OSGI-INF/b.xml,OSGI-INF/a.xml,OSGI-INF/c.xml");
		Jar b = builder.build();

		String exb = (String) b.getManifest().getMainAttributes().getValue("Service-Component");

		Tree newer = differ.tree(b);
		Tree older = differ.tree(a);

		Diff diff = newer.diff(older);

		assertEquals(Delta.UNCHANGED, diff.getDelta());
	}
}
