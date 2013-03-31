package test.baseline;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.osgi.*;
import aQute.service.reporter.*;

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
	
	// Adding a method to a ProviderType produces a MINOR bump (1.0.0 -> 1.1.0)
	public static void testProviderTypeBump() throws Exception {
		Processor processor = new Processor();
		
		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		Jar older = new Jar(new File("test/api-orig.jar"));
		Jar newer = new Jar(new File("test/api-providerbump.jar"));

		Set<Info> infoSet = baseline.baseline(newer, older, null);
		
		assertEquals(1, infoSet.size());
		Info info = infoSet.iterator().next();
		
		assertTrue(info.mismatch);
		assertEquals("dummy.api", info.packageName);
		assertEquals("1.1.0", info.suggestedVersion.toString());
	}
	
	// Adding a method to a ConsumerType produces a MINOR bump (1.0.0 -> 2.0.0)
	public static void testConsumerTypeBump() throws Exception {
		Processor processor = new Processor();
		
		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		Jar older = new Jar(new File("test/api-orig.jar"));
		Jar newer = new Jar(new File("test/api-consumerbump.jar"));

		Set<Info> infoSet = baseline.baseline(newer, older, null);
		
		assertEquals(1, infoSet.size());
		Info info = infoSet.iterator().next();
		
		assertTrue(info.mismatch);
		assertEquals("dummy.api", info.packageName);
		assertEquals("2.0.0", info.suggestedVersion.toString());
	}
}
