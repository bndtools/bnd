package test.baseline;

import java.io.*;
import java.util.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.io.*;

public class BaselineTest extends TestCase {

	public static void testBaslineJar() throws Exception {
		// Workspace ws = new Workspace(new File("test/ws"));
		//
		// Project p3 = ws.getProject("p3");
		//
		// ProjectBuilder builder = (ProjectBuilder)
		// p3.getBuilder(null).getSubBuilder();
		// builder.setBundleSymbolicName("p3");
		//
		// // Nothing specified
		// Jar jar = builder.getBaselineJar(false);
		// assertNull(jar);
		//
		// jar = builder.getBaselineJar(true);
		// assertEquals(".", jar.getName());
		//
		// // Fallback to release repo
		// builder.set("-releaserepo", "Repo");
		// jar = builder.getBaselineJar(false);
		// assertNull(jar);
		//
		// jar = builder.getBaselineJar(true);
		// assertEquals("p3", jar.getBsn());
		// assertEquals("1.0.1", jar.getVersion());
		//
		// // -baselinerepo specified
		// builder.set("-baselinerepo", "Release");
		// jar = builder.getBaselineJar(false);
		// assertEquals("p3", jar.getBsn());
		// assertEquals("1.2.0", jar.getVersion());
		//
		// jar = builder.getBaselineJar(true);
		// assertEquals("p3", jar.getBsn());
		// assertEquals("1.2.0", jar.getVersion());
		//
		// // -baseline specified
		// builder.set("-baseline", "p3;version=1.1.0");
		// jar = builder.getBaselineJar(false);
		// assertEquals("p3", jar.getBsn());
		// assertEquals("1.1.0", jar.getVersion());
		//
		// jar = builder.getBaselineJar(true);
		// assertEquals("p3", jar.getBsn());
		// assertEquals("1.1.0", jar.getVersion());

	}

	/**
	 * In repo:
	 * 
	 * <pre>
	 * p3-1.1.0.jar
	 * p3-1.2.0.jar
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public static void testRepository() throws Exception {
		Workspace ws = new Workspace(new File("test/ws"));

		Jar v1_2_0_a = mock(Jar.class);
		when(v1_2_0_a.getVersion()).thenReturn("1.2.0.b");
		when(v1_2_0_a.getBsn()).thenReturn("p3");

		RepositoryPlugin repo = mock(RepositoryPlugin.class);
		ws.addBasicPlugin(repo);
		when(repo.get(anyString(), any(Version.class), any(Map.class))).thenReturn(
				IO.getFile("test/ws/cnf/releaserepo/p3/p3-1.2.0.jar"));
		System.out.println(repo.get("p3", new Version("1.2.0.b"), new Attrs()));

		when(repo.canWrite()).thenReturn(true);
		when(repo.getName()).thenReturn("Baseline");
		when(repo.versions("p3")).thenReturn(
				new SortedList<Version>(new Version("1.1.0.a"), new Version("1.1.0.b"), new Version("1.2.0.a"),
						new Version("1.2.0.b")));

		Project p3 = ws.getProject("p3");
		p3.setBundleVersion("1.3.0");
		ProjectBuilder builder = (ProjectBuilder) p3.getBuilder(null).getSubBuilder();
		builder.setProperty(Constants.BASELINE, "*");
		builder.setProperty(Constants.BASELINEREPO, "Baseline");

		// Nothing specified
		Jar jar = builder.getBaselineJar();
		assertEquals("1.2.0", new Version(jar.getVersion()).getWithoutQualifier().toString());

		if (!builder.check())
			fail();
		{
			// check for error when repository contains later versions
			builder = (ProjectBuilder) p3.getBuilder(null).getSubBuilder();
			builder.setBundleVersion("1.1.3");
			builder.setTrace(true);
			builder.setProperty(Constants.BASELINE, "*");
			builder.setProperty(Constants.BASELINEREPO, "Baseline");
			jar = builder.getBaselineJar();
			assertNull(jar);

			if (!builder.check("The baseline version 1.2.0.b is higher or equal than the current version 1.1.3 for p3"))
				fail();
		}
		{
			// check for no error when repository has the same version
			builder = (ProjectBuilder) p3.getBuilder(null).getSubBuilder();
			builder.setBundleVersion("1.2.0.b");
			builder.setTrace(true);
			builder.setProperty(Constants.BASELINE, "*");
			builder.setProperty(Constants.BASELINEREPO, "Baseline");
			jar = builder.getBaselineJar();
			assertNotNull(jar);

			if (!builder.check())
				fail();

		}
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
