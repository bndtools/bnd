package test.component;

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
public class ComponentOrderingTest {

	@Test
	public void testOrdering() throws Exception {
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

		DiffPluginImpl differ = new DiffPluginImpl();
		Tree newer = differ.tree(b);
		Tree older = differ.tree(a);

		Diff diff = newer.diff(older);

		assertEquals(Delta.UNCHANGED, diff.getDelta());
	}
}
