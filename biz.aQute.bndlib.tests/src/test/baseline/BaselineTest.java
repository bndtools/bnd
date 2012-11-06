package test.baseline;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;

public class BaselineTest extends TestCase {

	public static void testBaslineJar() throws Exception {
		Workspace ws = new Workspace(new File("test/ws"));

		Project p3 = ws.getProject("p3");

		ProjectBuilder builder = (ProjectBuilder) p3.getBuilder(null).getSubBuilder();
		builder.setBundleSymbolicName("p3");

		// Nothing specified
		Jar jar = builder.getBaselineJar(false);
		assertNull(jar);

		jar = builder.getBaselineJar(true);
		assertEquals(".", jar.getName());

		// Fallback to release repo
		builder.set("-releaserepo", "Repo");
		jar = builder.getBaselineJar(false);
		assertNull(jar);

		jar = builder.getBaselineJar(true);
		assertEquals("p3", jar.getBsn());
		assertEquals("1.0.1", jar.getVersion());

		// -baselinerepo specified
		builder.set("-baselinerepo", "Release");
		jar = builder.getBaselineJar(false);
		assertEquals("p3", jar.getBsn());
		assertEquals("1.2.0", jar.getVersion());

		jar = builder.getBaselineJar(true);
		assertEquals("p3", jar.getBsn());
		assertEquals("1.2.0", jar.getVersion());

		// -baseline specified
		builder.set("-baseline", "p3;version=1.1.0");
		jar = builder.getBaselineJar(false);
		assertEquals("p3", jar.getBsn());
		assertEquals("1.1.0", jar.getVersion());

		jar = builder.getBaselineJar(true);
		assertEquals("p3", jar.getBsn());
		assertEquals("1.1.0", jar.getVersion());

	}
}
