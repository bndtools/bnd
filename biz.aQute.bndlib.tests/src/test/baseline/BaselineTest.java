package test.baseline;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.diff.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;

@SuppressWarnings("resource")
public class BaselineTest extends TestCase {
	private static void reallyClean(Workspace ws) throws Exception {
		String wsName = ws.getBase().getName();
		for (Project project : ws.getAllProjects()) {
			if (("p2".equals(project.getName()) && "ws".equals(wsName))) {
				File output = project.getSrcOutput().getAbsoluteFile();
				if (output.isDirectory() && output.getParentFile() != null) {
					IO.delete(output);
				}
			} else {
				project.clean();

				File target = project.getTargetDir();
				if (target.isDirectory() && target.getParentFile() != null) {
					IO.delete(target);
				}
				File output = project.getSrcOutput().getAbsoluteFile();
				if (output.isDirectory() && output.getParentFile() != null) {
					IO.delete(output);
				}
			}
		}
		IO.delete(ws.getFile("cnf/cache"));
	}

	public void tearDown() throws Exception {
		reallyClean(new Workspace(new File("testresources/ws")));
	}

	public static void testBaslineJar() throws Exception {
		// Workspace ws = new Workspace(new File("testresources/ws"));
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
	 * When a JAR is build the manifest is not set in the resources but in a
	 * instance var.
	 * 
	 * @throws Exception
	 */
	public void testPrematureJar() throws Exception {
		Builder b1 = new Builder();
		b1.addClasspath(IO.getFile(new File(""), "jar/osgi.jar"));
		b1.setProperty(Constants.BUNDLE_VERSION, "1.0.0.${tstamp}");
		b1.setExportPackage("org.osgi.service.event");
		Jar j1 = b1.build();
		System.out.println(j1.getResources().keySet());
		assertTrue(b1.check());

		File tmp = new File("tmp.jar");
		try {
			j1.write(tmp);
			Jar j11 = new Jar(tmp);

			Thread.sleep(2000);

			Builder b2 = new Builder();
			b2.addClasspath(IO.getFile(new File(""), "jar/osgi.jar"));
			b2.setProperty(Constants.BUNDLE_VERSION, "1.0.0.${tstamp}");
			b2.setExportPackage("org.osgi.service.event");
			Jar j2 = b2.build();
			assertTrue(b2.check());
			System.out.println(j2.getResources().keySet());

			DiffPluginImpl differ = new DiffPluginImpl();

			ReporterAdapter ra = new ReporterAdapter();
			Baseline baseline = new Baseline(ra, differ);
			ra.setTrace(true);
			ra.setPedantic(true);
			Set<Info> infos = baseline.baseline(j2, j11, null);
			print(baseline.getDiff(), " ");

			assertEquals(Delta.UNCHANGED, baseline.getDiff().getDelta());
		}
		finally {
			tmp.delete();
		}
	}

	static Pattern	VERSION_HEADER_P	= Pattern.compile("Bundle-Header:(" + Verifier.VERSION_STRING + ")",
												Pattern.CASE_INSENSITIVE);

	void print(Diff diff, String indent) {
		if (diff.getDelta() == Delta.UNCHANGED)
			return;

		System.out.println(indent + " " + diff);
		for (Diff sub : diff.getChildren()) {
			print(sub, indent + " ");
		}
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
		Workspace ws = new Workspace(new File("testresources/ws"));

		Jar v1_2_0_a = mock(Jar.class);
		when(v1_2_0_a.getVersion()).thenReturn("1.2.0.b");
		when(v1_2_0_a.getBsn()).thenReturn("p3");

		RepositoryPlugin repo = mock(RepositoryPlugin.class);
		ws.addBasicPlugin(repo);
		when(repo.get(anyString(), any(Version.class), any(Map.class))).thenReturn(
				IO.getFile("testresources/ws/cnf/releaserepo/p3/p3-1.2.0.jar"));
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

			if (!builder.check("The baseline version 1.2.0.b is higher than the current version 1.1.3 for p3"))
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
		{
			// check for no error when repository has the same version
			builder = (ProjectBuilder) p3.getBuilder(null).getSubBuilder();
			builder.setBundleVersion("1.2.0.b");
			builder.setTrace(true);
			builder.setProperty(Constants.BASELINE, "*");
			builder.setProperty(Constants.BASELINEREPO, "Baseline");
			builder.build();

			if (!builder.check("The bundle version \\(1.2.0/1.2.0\\) is too low, must be at least 1.3.0"))
				fail();

		}
	}

	/**
	 * Check what happens when there is nothing in the repo ... We do not
	 * generate an error when version <=1.0.0, otherwise we generate an error.
	 * 
	 * @throws Exception
	 */
	public static void testNothingInRepo() throws Exception {
		File tmp = new File("tmp");
		tmp.mkdirs();
		try {
			IO.copy(new File("testresources/ws"), tmp);
			Workspace ws = new Workspace(tmp);
			RepositoryPlugin repo = mock(RepositoryPlugin.class);
			when(repo.canWrite()).thenReturn(true);
			when(repo.getName()).thenReturn("Baseline");
			when(repo.versions("p3")).thenReturn(new TreeSet<Version>());
			ws.addBasicPlugin(repo);
			Project p3 = ws.getProject("p3");
			p3.setProperty(Constants.BASELINE, "*");
			p3.setProperty(Constants.BASELINEREPO, "Baseline");
			p3.setBundleVersion("0");
			p3.build();
			assertTrue(p3.check());

			p3.setBundleVersion("1.0.0.XXXXXX");
			p3.build();
			assertTrue(p3.check());

			p3.setBundleVersion("5");
			p3.build();
			assertTrue(p3.check("There is no baseline for p3 in the baseline repo"));
		}
		finally {
			IO.delete(tmp);
		}
	}

	// Adding a method to a ProviderType produces a MINOR bump (1.0.0 -> 1.1.0)
	public static void testProviderTypeBump() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		Jar older = new Jar(new File("testresources/api-orig.jar"));
		Jar newer = new Jar(new File("testresources/api-providerbump.jar"));

		Set<Info> infoSet = baseline.baseline(newer, older, null);
		System.out.println(differ.tree(newer).get("<api>"));

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

		Jar older = new Jar(new File("testresources/api-orig.jar"));
		Jar newer = new Jar(new File("testresources/api-consumerbump.jar"));

		Set<Info> infoSet = baseline.baseline(newer, older, null);

		assertEquals(1, infoSet.size());
		Info info = infoSet.iterator().next();

		assertTrue(info.mismatch);
		assertEquals("dummy.api", info.packageName);
		assertEquals("2.0.0", info.suggestedVersion.toString());
	}
}
