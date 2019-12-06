package test.baseline;

import static aQute.bnd.osgi.Constants.BUNDLE_SYMBOLICNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class BaselineTest extends TestCase {
	File		tmp;
	Workspace	workspace;

	private Workspace getWorkspace() throws Exception {
		if (workspace != null)
			return workspace;

		IO.copy(IO.getFile("testresources/ws"), tmp);
		return workspace = new Workspace(tmp);
	}

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + getClass().getName() + "/" + getName())
			.getAbsoluteFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);
	}

	@Override
	protected void tearDown() throws Exception {
		IO.delete(tmp);
		workspace = null;
	}

	/**
	 * Test 2 jars compiled with different compilers
	 */
	public void testCompilerEnumDifference() throws Exception {
		DiffPluginImpl diff = new DiffPluginImpl();
		try (Jar ecj = new Jar(IO.getFile("jar/baseline/com.example.baseline.ecj.jar"));
			Jar javac = new Jar(IO.getFile("jar/baseline/com.example.baseline.javac.jar"));) {

			Tree tecj = diff.tree(ecj);
			Tree tjavac = diff.tree(javac);
			Diff d = tecj.diff(tjavac);
			assertEquals(Delta.UNCHANGED, d.getDelta());
		}
	}

	/**
	 * Test skipping classes when there is source
	 */
	public void testClassesDiffWithSource() throws Exception {
		DiffPluginImpl diff = new DiffPluginImpl();
		try (Jar jar = new Jar(IO.getFile("jar/osgi.jar")); Jar out = new Jar(".");) {
			out.putResource("OSGI-OPT/src/org/osgi/application/ApplicationContext.java",
				jar.getResource("OSGI-OPT/src/org/osgi/application/ApplicationContext.java"));
			out.putResource("org/osgi/application/ApplicationContext.class",
				jar.getResource("org/osgi/application/ApplicationContext.class"));
			try (Analyzer a = new Analyzer(out)) {
				a.addClasspath(out);
				Tree tree = diff.tree(a);

				Tree src = tree.get("<resources>")
					.get("OSGI-OPT/src/org/osgi/application/ApplicationContext.java")
					.getChildren()[0];

				assertNotNull(src);

				assertNull(tree.get("<resources>")
					.get("org/osgi/application/ApplicationContext.class"));
			}
		}
	}

	public void testClassesDiffWithoutSource() throws Exception {
		DiffPluginImpl diff = new DiffPluginImpl();
		try (Jar jar = new Jar(IO.getFile("jar/osgi.jar")); Jar out = new Jar(".");) {
			for (String path : jar.getResources()
				.keySet()) {
				if (!path.startsWith("OSGI-OPT/src/"))
					out.putResource(path, jar.getResource(path));
			}
			try (Analyzer a = new Analyzer(out)) {
				a.addClasspath(out);
				Tree tree = diff.tree(a);
				assertNull(tree.get("<resources>")
					.get("OSGI-OPT/src/org/osgi/application/ApplicationContext.java"));
				assertNotNull(tree.get("<resources>")
					.get("org/osgi/application/ApplicationContext.class"));
			}
		}
	}

	public void testJava8DefaultMethods() throws Exception {
		try (Builder older = new Builder(); Builder newer = new Builder();) {
			older.addClasspath(IO.getFile("java8/older/bin"));
			older.setExportPackage("*;version=1.0");
			newer.addClasspath(IO.getFile("java8/newer/bin"));
			newer.setExportPackage("*;version=1.0");
			try (Jar o = older.build(); Jar n = newer.build();) {
				assertTrue(older.check());
				assertTrue(newer.check());

				DiffPluginImpl differ = new DiffPluginImpl();
				Baseline baseline = new Baseline(older, differ);

				Set<Info> infoSet = baseline.baseline(n, o, null);
				assertEquals(1, infoSet.size());
				for (Info info : infoSet) {
					assertTrue(info.mismatch);
					assertEquals(new Version(1, 1, 0), info.suggestedVersion);
					assertEquals(info.packageName, "api_default_methods");
				}
			}
		}
	}

	public void testNoMismatchForZeroMajor() throws Exception {
		try (Builder older = new Builder(); Builder newer = new Builder();) {
			older.addClasspath(IO.getFile("java8/older/bin"));
			older.setExportPackage("*;version=0.1");
			newer.addClasspath(IO.getFile("java8/newer/bin"));
			newer.setExportPackage("*;version=0.1");
			try (Jar o = older.build(); Jar n = newer.build();) {
				assertTrue(older.check());
				assertTrue(newer.check());

				DiffPluginImpl differ = new DiffPluginImpl();
				Baseline baseline = new Baseline(older, differ);

				Set<Info> infoSet = baseline.baseline(n, o, null);
				assertEquals(1, infoSet.size());
				for (Info info : infoSet) {
					assertFalse(info.mismatch);
					assertEquals(new Version(0, 2, 0), info.suggestedVersion);
					assertEquals(info.packageName, "api_default_methods");
				}
			}
		}
	}

	/**
	 * Check if we can ignore resources in the baseline. First build two jars
	 * that are identical except for the b/b resource. Then do baseline on them.
	 */
	public void testIgnoreResourceDiff() throws Exception {
		Processor processor = new Processor();
		DiffPluginImpl differ = new DiffPluginImpl();
		differ.setIgnore("b/b");
		Baseline baseline = new Baseline(processor, differ);

		try (Builder a = new Builder(); Builder b = new Builder();) {
			a.setProperty("-includeresource", "a/a;literal='aa',b/b;literal='bb'");
			a.setProperty("-resourceonly", "true");
			b.setProperty("-includeresource", "a/a;literal='aa',b/b;literal='bbb'");
			b.setProperty("-resourceonly", "true");
			try (Jar aj = a.build(); Jar bj = b.build();) {
				Set<Info> infoSet = baseline.baseline(aj, bj, null);

				BundleInfo binfo = baseline.getBundleInfo();
				assertFalse(binfo.mismatch);
			}
		}
	}

	public static void testBaslineJar() throws Exception {
		// Workspace ws = new Workspace(IO.getFile("testresources/ws"));
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
		File file = IO.getFile(new File(""), "jar/osgi.jar");
		try (Builder b1 = new Builder(); Builder b2 = new Builder();) {
			b1.addClasspath(file);
			b1.setProperty(Constants.BUNDLE_VERSION, "1.0.0.${tstamp}");
			b1.setExportPackage("org.osgi.service.event");
			try (Jar j1 = b1.build();) {
				assertTrue(b1.check());

				File tmp = new File("tmp.jar");
				j1.write(tmp);
				try (Jar j11 = new Jar(tmp);) {
					Thread.sleep(2000);

					b2.addClasspath(file);
					b2.setProperty(Constants.BUNDLE_VERSION, "1.0.0.${tstamp}");
					b2.setExportPackage("org.osgi.service.event");

					try (Jar j2 = b2.build();) {
						assertTrue(b2.check());

						DiffPluginImpl differ = new DiffPluginImpl();

						ReporterAdapter ra = new ReporterAdapter();
						Baseline baseline = new Baseline(ra, differ);
						ra.setTrace(true);
						ra.setPedantic(true);
						Set<Info> infos = baseline.baseline(j2, j11, null);
						print(baseline.getDiff(), " ");

						assertEquals(Delta.UNCHANGED, baseline.getDiff()
							.getDelta());
					}
				} finally {
					tmp.delete();
				}
			}
		}
	}

	static Pattern VERSION_HEADER_P = Pattern.compile("Bundle-Header:(" + Verifier.VERSION_STRING + ")",
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
	 *  p3-1.1.0.jar p3-1.2.0.jar
	 * </pre>
	 *
	 * @throws Exception
	 */
	public void testRepository() throws Exception {
		Jar v1_2_0_a = mock(Jar.class);
		when(v1_2_0_a.getVersion()).thenReturn("1.2.0.b");
		when(v1_2_0_a.getBsn()).thenReturn("p3");

		RepositoryPlugin repo = mock(RepositoryPlugin.class);
		when(repo.get(anyString(), any(Version.class), anyMap()))
			.thenReturn(IO.getFile("testresources/ws/cnf/releaserepo/p3/p3-1.2.0.jar"));
		System.out.printf("get %s%n", repo.get("p3", new Version("1.2.0.b"), new HashMap<>()));

		when(repo.canWrite()).thenReturn(true);
		when(repo.getName()).thenReturn("Baseline");
		when(repo.versions("p3")).thenReturn(new SortedList<>(new Version("1.1.0.a"), new Version("1.1.0.b"),
			new Version("1.2.0.a"), new Version("1.2.0.b")));
		getWorkspace().addBasicPlugin(repo);

		Project p3 = getWorkspace().getProject("p3");
		p3.setBundleVersion("1.3.0");
		ProjectBuilder builder = (ProjectBuilder) p3.getBuilder(null)
			.getSubBuilder();
		builder.setProperty(Constants.BASELINE, "*");
		builder.setProperty(Constants.BASELINEREPO, "Baseline");

		// Nothing specified
		Jar jar = builder.getBaselineJar();
		assertEquals("1.2.0", new Version(jar.getVersion()).getWithoutQualifier()
			.toString());

		if (!builder.check())
			fail(builder.getErrors()
				.toString());
		{
			// check for error when repository contains later versions
			builder = (ProjectBuilder) p3.getBuilder(null)
				.getSubBuilder();
			builder.setBundleVersion("1.1.3");
			builder.setTrace(true);
			builder.setProperty(Constants.BASELINE, "*");
			builder.setProperty(Constants.BASELINEREPO, "Baseline");
			jar = builder.getBaselineJar();
			assertNull(jar);

			if (!builder.check("The baseline version 1.2.0.b is higher than the current version 1.1.3 for p3"))
				fail(builder.getErrors()
					.toString());
		}
		{
			// check for no error when repository has the same version
			builder = (ProjectBuilder) p3.getBuilder(null)
				.getSubBuilder();
			builder.setBundleVersion("1.2.0.b");
			builder.setTrace(true);
			builder.setProperty(Constants.BASELINE, "*");
			builder.setProperty(Constants.BASELINEREPO, "Baseline");
			jar = builder.getBaselineJar();
			assertNotNull(jar);

			if (!builder.check())
				fail(builder.getErrors()
					.toString());

		}
		{
			// check for no error when repository has the same version
			builder = (ProjectBuilder) p3.getBuilder(null)
				.getSubBuilder();
			builder.setBundleVersion("1.2.0.b");
			builder.setTrace(true);
			builder.setProperty(Constants.BASELINE, "*");
			builder.setProperty(Constants.BASELINEREPO, "Baseline");
			builder.build();

			if (!builder.check("The bundle version \\(1.2.0/1.2.0\\) is too low, must be at least 1.3.0"))
				fail(builder.getErrors()
					.toString());

		}
	}

	/**
	 * Check what happens when there is nothing in the repo ... We do not
	 * generate an error when version <=1.0.0, otherwise we generate an error.
	 *
	 * @throws Exception
	 */
	public void testNothingInRepo() throws Exception {
		File tmp = new File("tmp");
		tmp.mkdirs();
		try {
			RepositoryPlugin repo = mock(RepositoryPlugin.class);
			when(repo.canWrite()).thenReturn(true);
			when(repo.getName()).thenReturn("Baseline");
			when(repo.versions("p3")).thenReturn(new TreeSet<>());
			getWorkspace().addBasicPlugin(repo);
			Project p3 = getWorkspace().getProject("p3");
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
		} finally {
			IO.delete(tmp);
		}
	}

	// Adding a method to a ProviderType produces a MINOR bump (1.0.0 -> 1.1.0)
	public void testProviderTypeBump() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("testresources/api-orig.jar"));
			Jar newer = new Jar(IO.getFile("testresources/api-providerbump.jar"));) {

			Set<Info> infoSet = baseline.baseline(newer, older, null);
			System.out.println(differ.tree(newer)
				.get("<api>"));

			assertEquals(1, infoSet.size());
			Info info = infoSet.iterator()
				.next();

			assertTrue(info.mismatch);
			assertEquals("dummy.api", info.packageName);
			assertEquals("1.1.0", info.suggestedVersion.toString());
		}
	}

	// Adding a method to a ConsumerType produces a MINOR bump (1.0.0 -> 2.0.0)
	public static void testConsumerTypeBump() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("testresources/api-orig.jar"));
			Jar newer = new Jar(IO.getFile("testresources/api-consumerbump.jar"));) {

			Set<Info> infoSet = baseline.baseline(newer, older, null);

			assertEquals(1, infoSet.size());
			Info info = infoSet.iterator()
				.next();

			assertTrue(info.mismatch);
			assertEquals("dummy.api", info.packageName);
			assertEquals("2.0.0", info.suggestedVersion.toString());
		}
	}

	// Adding a method to a ProviderType produces a MINOR bump (1.0.0 -> 1.1.0)
	public void testBundleVersionBump() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("testresources/api-orig.jar"));
			Jar newer = new Jar(IO.getFile("testresources/api-providerbump.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertTrue(bundleInfo.mismatch);
			assertEquals("1.1.0", bundleInfo.suggestedVersion.toString());
		}
	}

	// Adding a method to a ProviderType produces a MINOR bump (1.0.0 -> 1.1.0)
	public void testBundleVersionBumpDifferentSymbolicNames() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("testresources/api-orig.jar"));
			Jar newer = new Jar(IO.getFile("testresources/api-providerbump.jar"));) {

			newer.getManifest()
				.getMainAttributes()
				.putValue(BUNDLE_SYMBOLICNAME, "a.different.name");

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertFalse(bundleInfo.mismatch);
			assertEquals(newer.getVersion(), bundleInfo.suggestedVersion.toString());
		}
	}

	// Adding a method to an exported class produces a MINOR bump (1.0.0 ->
	// 1.1.0)
	public void testMinorChange() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("testresources/minor-and-removed-change-1.0.0.jar"));
			Jar newer = new Jar(IO.getFile("testresources/minor-change-1.0.1.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertTrue(bundleInfo.mismatch);
			assertEquals("1.1.0", bundleInfo.suggestedVersion.toString());
		}
	}

	// Adding a method to an exported class and unexporting a package produces a
	// MINOR bump (1.0.0 -> 1.1.0)
	public void testMinorAndRemovedChange() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("testresources/minor-and-removed-change-1.0.0.jar"));
			Jar newer = new Jar(IO.getFile("testresources/minor-and-removed-change-1.0.1.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertTrue(bundleInfo.mismatch);
			assertEquals("2.0.0", bundleInfo.suggestedVersion.toString());
		}
	}

	// Deleting a protected field on a ProviderType API class produces a MINOR
	// bump (1.0.0 -> 1.1.0)
	public void testProviderProtectedFieldRemovedChange() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("jar/baseline/provider-deletion-1.0.0.jar"));
			Jar newer = new Jar(IO.getFile("jar/baseline/provider-deletion-1.1.0.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertFalse(bundleInfo.mismatch);
			assertEquals("1.1.0", bundleInfo.suggestedVersion.toString());

			Set<Info> packageInfos = baseline.getPackageInfos();

			assertEquals(1, packageInfos.size());

			Info change = packageInfos.iterator()
				.next();
			assertTrue(change.mismatch);
			assertEquals("bnd.baseline.test", change.packageName);
			assertEquals("1.1.0", change.suggestedVersion.toString());
		}
	}

	// Moving a package from the root into a jar on the Bundle-ClassPath
	// should not result in DELETED
	public void testMovePackageToBundleClassPath() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("jar/baseline/com.liferay.calendar.api-2.0.5.jar"));
			Jar newer = new Jar(IO.getFile("jar/baseline/com.liferay.calendar.api-2.1.0.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertFalse(bundleInfo.mismatch);
			assertEquals("2.1.0", bundleInfo.suggestedVersion.toString());

			Set<Info> packageInfos = baseline.getPackageInfos();

			assertEquals(12, packageInfos.size());

			Info change = packageInfos.iterator()
				.next();
			assertFalse(change.mismatch);
			assertEquals("com.google.ical.iter", change.packageName);
			assertEquals("20110304.0.0", change.suggestedVersion.toString());
		}
	}

	// This tests the scenario where a super type is injected into the class
	// hierarchy but the super class comes from outside the bundle so that the
	// baseline cannot find it. Since the class hierarchy was cut off, the
	// baseline would _forget_ that every class inherits from Object, and _lose_
	// Object's methods if not directly implemented.
	public void testCutOffInheritance() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("jar/baseline/inheritance-change-1.0.0.jar"));
			Jar newer = new Jar(IO.getFile("jar/baseline/inheritance-change-1.1.0.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertFalse(bundleInfo.mismatch);
			assertEquals("1.1.0", bundleInfo.suggestedVersion.toString());

			Set<Info> packageInfos = baseline.getPackageInfos();

			assertEquals(1, packageInfos.size());

			Info change = packageInfos.iterator()
				.next();
			assertFalse(change.mismatch);
			assertEquals("example", change.packageName);
			assertEquals("1.1.0", change.suggestedVersion.toString());

			Diff packageDiff = change.packageDiff;

			Collection<? extends Diff> children = packageDiff.getChildren();

			assertEquals(5, children.size());

			Iterator<? extends Diff> iterator = children.iterator();

			Diff diff = iterator.next();
			assertEquals(Delta.MICRO, diff.getDelta());
			diff = iterator.next();
			assertEquals(Delta.MICRO, diff.getDelta());
			diff = iterator.next();
			assertEquals(Delta.MINOR, diff.getDelta());
		}
	}

	// This tests the scenario where the return type of an interface method is
	// expanded through generics.
	// e.g. from:
	// Foo getFoo();
	// to:
	// <T extends Foo> T getFoo();
	// or:
	// <T extends Foo & Comparable<Foo>> T getFoo();
	public void testExpandErasureOfMethodReturn() throws Exception {
		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		try (Jar older = new Jar(IO.getFile("jar/baseline/expanding-erasure-1.0.0.jar"));
			Jar newer = new Jar(IO.getFile("jar/baseline/expanding-erasure-1.1.0.jar"));) {

			baseline.baseline(newer, older, null);

			BundleInfo bundleInfo = baseline.getBundleInfo();

			assertFalse(bundleInfo.mismatch);
			assertEquals("1.1.0", bundleInfo.suggestedVersion.toString());

			Set<Info> packageInfos = baseline.getPackageInfos();

			assertEquals(1, packageInfos.size());

			Info change = packageInfos.iterator()
				.next();
			assertFalse(change.mismatch);
			assertEquals("bnd.test", change.packageName);
			assertEquals("1.0.0", change.suggestedVersion.toString());

			Diff packageDiff = change.packageDiff;

			Collection<? extends Diff> children = packageDiff.getChildren();

			assertEquals(3, children.size());

			Iterator<? extends Diff> iterator = children.iterator();

			Diff diff = iterator.next();
			assertEquals(Delta.UNCHANGED, diff.getDelta());
		}
	}

}
