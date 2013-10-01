package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.eclipse.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.deployer.*;
import aQute.lib.io.*;

@SuppressWarnings("resource")
public class ProjectTest extends TestCase {

	/**
	 * https://github.com/bndtools/bnd/issues/395
	 * 
	 * Repo macro does not refer to anything
	 */

	public static void testRepoMacro2() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project top = ws.getProject("p2");
		top.addClasspath(top.getOutput());
		
		top.setProperty("a", "${repo;org.apache.felix.configadmin;latest}");
		assertTrue(top.getProperty("a").endsWith("org.apache.felix.configadmin/org.apache.felix.configadmin-1.2.0.jar"));
		
		top.setProperty("a", "${repo;IdoNotExist;latest}");
		top.getProperty("a");
		assertTrue(top.check("macro refers to an artifact IdoNotExist-latest.*that has an error"));
		assertEquals("", top.getProperty("a"));
	}
	
	/**
	 * Two subsequent builds should not change the last modified if none of the
	 * source inputs have been modified.
	 * 
	 * @throws Exception
	 */
	public static void testLastModified() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.deleteWithException(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());
		try {
			IO.copy(new File("testresources/ws"), tmp);

			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("p6");
			File bnd = new File("testresources/ws/p6/bnd.bnd");
			assertTrue(bnd.exists());

			project.clean();
			File pt = project.getTarget();
			if (!pt.exists() && !pt.mkdirs()) {
				throw new IOException("Could not create directory " + pt);
			}
			try {
				// Now we build it.
				File[] files = project.build();
				assertTrue(project.check());
				assertNotNull(files);
				assertEquals(1, files.length);

				Jar older = new Jar(files[0]);
				byte[] olderDigest = older.getTimelessDigest();
				older.close();
				System.out.println();
				Thread.sleep(3000); // Ensure system time granularity is < than
									// wait

				files[0].delete();

				project.build();
				assertTrue(project.check());
				assertNotNull(files);
				assertEquals(1, files.length);

				Jar newer = new Jar(files[0]);
				byte[] newerDigest = newer.getTimelessDigest();
				newer.close();

				assertTrue(Arrays.equals(olderDigest, newerDigest));
			}
			finally {
				project.clean();
			}
		}
		finally {
			IO.delete(tmp);
		}
	}

	/**
	 * #194 StackOverflowError when -runbundles in bnd.bnd refers to itself
	 */

	public static void testProjectReferringToItself() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project top = ws.getProject("bug194");
		top.addClasspath(top.getOutput());
		assertTrue(top.check("Circular dependency context"));
	}

	/**
	 * Test if you can add directories and files to the classpath. Originally
	 * checked only for files
	 */

	public static void testAddDirToClasspath() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project top = ws.getProject("p1");
		top.addClasspath(top.getOutput());
		assertTrue(top.check());
	}

	/**
	 * Test bnd.bnd of project `foo`: `-runbundles: foo;version=latest`
	 */
	public static void testRunBundlesContainsSelf() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project top = ws.getProject("p1");
		top.setProperty("-runbundles", "p1;version=latest");
		top.setChanged();
		top.isStale();
		Collection<Container> runbundles = top.getRunbundles();
		assertTrue(top.check("Circular dependency"));
		assertNotNull(runbundles);
		assertEquals(0, runbundles.size());
	}

	/**
	 * Test 2 equal bsns but diff. versions
	 */

	public static void testSameBsnRunBundles() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project top = ws.getProject("p1");
		top.setProperty("-runbundles",
				"org.apache.felix.configadmin;version='[1.0.1,1.0.1]',org.apache.felix.configadmin;version='[1.1.0,1.1.0]'");
		Collection<Container> runbundles = top.getRunbundles();
		assertTrue(top.check());
		assertNotNull(runbundles);
		assertEquals(2, runbundles.size());
	}

	/**
	 * Duplicates in runbundles gave a bad error, should be ignored
	 */

	public static void testRunbundleDuplicates() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project top = ws.getProject("p1");
		top.setPedantic(true);
		top.clear();
		top.setProperty("-runbundles", "org.apache.felix.configadmin,org.apache.felix.configadmin");
		Collection<Container> runbundles = top.getRunbundles();
		assertTrue(top.check("Multiple bundles with the same final URL"));
		assertNotNull(runbundles);
		assertEquals(1, runbundles.size());
	}

	/**
	 * Check isStale
	 */

	public static void testIsStale() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws"));
		ws.setOffline(false);
		Project top = ws.getProject("p-stale");
		assertNotNull(top);
		top.build();
		Project bottom = ws.getProject("p-stale-dep");
		assertNotNull(bottom);
		bottom.build();

		long lastModified = bottom.lastModified();
		top.getPropertiesFile().setLastModified(lastModified + 1000);

		stale(top, true);
		stale(bottom, true);
		assertTrue(top.isStale());
		assertTrue(bottom.isStale());

		stale(top, false);
		stale(bottom, true);
		assertTrue(top.isStale());
		assertTrue(bottom.isStale());

		stale(top, true);
		stale(bottom, false);
		assertTrue(top.isStale());
		assertFalse(bottom.isStale());

		// Thread.sleep(1000);
		// stale(top, false);
		// stale(bottom, false);
		// assertFalse(top.isStale());
		// assertFalse(bottom.isStale());
	}

	private static void stale(Project project, boolean b) throws Exception {
		File file = project.getBuildFiles(false)[0];
		if (b)
			file.setLastModified(project.lastModified() - 10000);
		else
			file.setLastModified(project.lastModified() + 10000);
	}

	/**
	 * Check multiple repos
	 * 
	 * @throws Exception
	 */
	public static void testMultipleRepos() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws"));
		Project project = ws.getProject("p1");
		project.setPedantic(true);
		System.err.println(project.getBundle("org.apache.felix.configadmin", "1.1.0", Strategy.EXACT, null));
		System.err.println(project.getBundle("org.apache.felix.configadmin", "1.1.0", Strategy.HIGHEST, null));
		System.err.println(project.getBundle("org.apache.felix.configadmin", "1.1.0", Strategy.LOWEST, null));

		List<Container> bundles = project.getBundles(Strategy.LOWEST,
				"org.apache.felix.configadmin;version=1.1.0,org.apache.felix.configadmin;version=1.1.0", "test");
		assertTrue(project.check("Multiple bundles with the same final URL"));
		assertEquals(1, bundles.size());
	}

	/**
	 * Check if the getSubBuilders properly predicts the output.
	 */

	public static void testSubBuilders() throws Exception {
		File tmp = new File("tmp");
		tmp.mkdirs();
		try {
			IO.copy(new File("testresources/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("p4-sub");

			Collection< ? extends Builder> bs = project.getSubBuilders();
			assertNotNull(bs);
			assertEquals(3, bs.size());
			Set<String> names = new HashSet<String>();
			for (Builder b : bs) {
				names.add(b.getBsn());
			}
			assertTrue(names.contains("p4-sub.a"));
			assertTrue(names.contains("p4-sub.b"));
			assertTrue(names.contains("p4-sub.c"));

			File[] files = project.build();
			assertTrue(project.check());

			System.err.println(Processor.join(project.getErrors(), "\n"));
			System.err.println(Processor.join(project.getWarnings(), "\n"));
			assertEquals(0, project.getErrors().size());
			assertEquals(0, project.getWarnings().size());
			assertNotNull(files);
			assertEquals(3, files.length);
			for (File file : files) {
				Jar jar = new Jar(file);
				Manifest m = jar.getManifest();
				assertTrue(names.contains(m.getMainAttributes().getValue("Bundle-SymbolicName")));
			}
		}
		finally {
			IO.deleteWithException(tmp);
		}
	}

	/**
	 * Tests the handling of the -sub facility
	 * 
	 * @throws Exception
	 */

	public static void testSub() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws"));
		Project project = ws.getProject("p4-sub");
		File[] files = project.build();
		Arrays.sort(files);

		System.err.println(Processor.join(project.getErrors(), "\n"));
		System.err.println(Processor.join(project.getWarnings(), "\n"));

		assertEquals(0, project.getErrors().size());
		assertEquals(0, project.getWarnings().size());
		assertNotNull(files);
		assertEquals(3, files.length);

		Jar a = new Jar(files[0]);
		Jar b = new Jar(files[1]);
		Manifest ma = a.getManifest();
		Manifest mb = b.getManifest();

		assertEquals("base", ma.getMainAttributes().getValue("Base-Header"));
		assertEquals("base", mb.getMainAttributes().getValue("Base-Header"));
		assertEquals("a", ma.getMainAttributes().getValue("Sub-Header"));
		assertEquals("b", mb.getMainAttributes().getValue("Sub-Header"));
	}

	public static void testOutofDate() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws"));
		Project project = ws.getProject("p3");
		File bnd = new File("testresources/ws/p3/bnd.bnd");
		assertTrue(bnd.exists());

		project.clean();
		File pt = project.getTarget();
		if (!pt.exists() && !pt.mkdirs()) {
			throw new IOException("Could not create directory " + pt);
		}
		try {
			// Now we build it.
			File[] files = project.build();
			System.err.println(project.getErrors());
			System.err.println(project.getWarnings());
			assertTrue(project.isOk());
			assertNotNull(files);
			assertEquals(1, files.length);

			// Now we should not rebuild it
			long lastTime = files[0].lastModified();
			files = project.build();
			assertEquals(1, files.length);
			assertTrue(files[0].lastModified() == lastTime);

			Thread.sleep(2000);

			project.updateModified(System.currentTimeMillis(), "Testing");
			files = project.build();
			assertEquals(1, files.length);
			assertTrue("Must have newer files now", files[0].lastModified() > lastTime);
		}
		finally {
			project.clean();
		}
	}

	public static void testRepoMacro() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws"));
		Project project = ws.getProject("p2");
		System.err.println(project.getPlugins(FileRepo.class));
		String s = project.getReplacer().process(("${repo;libtest}"));
		System.err.println(s);
		assertTrue(s.contains("org.apache.felix.configadmin" + File.separator + "org.apache.felix.configadmin-1.2.0"));
		assertTrue(s.contains("org.apache.felix.ipojo" + File.separator + "org.apache.felix.ipojo-1.0.0.jar"));

		s = project.getReplacer().process(("${repo;libtestxyz}"));
		assertTrue(s.matches(""));

		s = project.getReplacer().process("${repo;org.apache.felix.configadmin;1.0.0;highest}");
		assertTrue(s.endsWith("org.apache.felix.configadmin-1.2.0.jar"));
		s = project.getReplacer().process("${repo;org.apache.felix.configadmin;1.0.0;lowest}");
		assertTrue(s.endsWith("org.apache.felix.configadmin-1.0.1.jar"));
	}

	public static void testClasspath() throws Exception {
		File project = new File("").getAbsoluteFile();
		File workspace = project.getParentFile();
		Processor processor = new Processor();
		EclipseClasspath p = new EclipseClasspath(processor, workspace, project);
		System.err.println(p.getDependents());
		System.err.println(p.getClasspath());
		System.err.println(p.getSourcepath());
		System.err.println(p.getOutput());
	}

	public static void testBump() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.deleteWithException(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("testresources/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("p1");
			int size = project.getProperties().size();
			Version old = new Version(project.getProperty("Bundle-Version"));
			System.err.println("Old version " + old);
			project.bump("=+0");
			Version newv = new Version(project.getProperty("Bundle-Version"));
			System.err.println("New version " + newv);
			assertEquals(old.getMajor(), newv.getMajor());
			assertEquals(old.getMinor() + 1, newv.getMinor());
			assertEquals(0, newv.getMicro());
			assertEquals(size, project.getProperties().size());
			assertEquals("sometime", newv.getQualifier());
		}
		finally {
			IO.deleteWithException(tmp);
		}
	}

	public static void testBumpIncludeFile() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.deleteWithException(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("testresources/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("bump-included");
			project.setTrace(true);
			Version old = new Version(project.getProperty("Bundle-Version"));
			assertEquals(new Version(1, 0, 0), old);
			project.bump("=+0");

			Processor processor = new Processor();
			processor.setProperties(project.getFile("include.txt"));

			Version newv = new Version(processor.getProperty("Bundle-Version"));
			System.err.println("New version " + newv);
			assertEquals(1, newv.getMajor());
			assertEquals(1, newv.getMinor());
			assertEquals(0, newv.getMicro());
		}
		finally {
			IO.deleteWithException(tmp);
		}
	}

	public static void testBumpSubBuilders() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.deleteWithException(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("testresources/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("bump-sub");
			project.setTrace(true);

			assertNull(project.getProperty("Bundle-Version"));

			project.bump("=+0");

			assertNull(project.getProperty("Bundle-Version"));

			for (Builder b : project.getSubBuilders()) {
				assertEquals(new Version(1, 1, 0), new Version(b.getVersion()));
			}
		}
		finally {
			IO.deleteWithException(tmp);
		}
	}

	public static void testRunBuilds() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws"));

		// Running a .bnd includes built bundles by default
		Project p1 = ws.getProject("p1");
		assertTrue(p1.getRunBuilds());

		// Can override the default by specifying -runbuilds: false
		Project p2 = ws.getProject("p2");
		assertFalse(p2.getRunBuilds());

		// Running a .bndrun DOES NOT include built bundles by default
		Project p1a = new Project(ws, new File("testresources/ws/p1"), new File("testresources/ws/p1/p1a.bndrun"));
		assertFalse(p1a.getRunBuilds());

		// ... unless we override the default by specifying -runbuilds: true
		Project p1b = new Project(ws, new File("testresources/ws/p1"), new File("testresources/ws/p1/p1b.bndrun"));
		assertTrue(p1b.getRunBuilds());
	}

	public static void testSetPackageVersion() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.deleteWithException(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("testresources/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("p5");
			project.setTrace(true);

			Version newVersion = new Version(2, 0, 0);

			// Package with no package info
			project.setPackageInfo("pkg1", newVersion);
			Version version = project.getPackageInfo("pkg1");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg1", true, false);

			// Package with package-info.java containing @Version("1.0.0")
			project.setPackageInfo("pkg2", newVersion);
			version = project.getPackageInfo("pkg2");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg2", false, true);

			// Package with package-info.java containing
			// @aQute.bnd.annotations.Version("1.0.0")
			project.setPackageInfo("pkg3", newVersion);
			version = project.getPackageInfo("pkg3");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg3", false, true);

			// Package with package-info.java containing
			// @aQute.bnd.annotations.Version(value="1.0.0")
			project.setPackageInfo("pkg4", newVersion);
			version = project.getPackageInfo("pkg4");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg4", false, true);

			// Package with package-info.java containing version + packageinfo
			project.setPackageInfo("pkg5", newVersion);
			version = project.getPackageInfo("pkg5");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg5", true, true);

			// Package with package-info.java NOT containing version +
			// packageinfo
			project.setPackageInfo("pkg6", newVersion);
			version = project.getPackageInfo("pkg6");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg6", true, true);

			// Package with package-info.java NOT containing version
			project.setPackageInfo("pkg7", newVersion);
			version = project.getPackageInfo("pkg7");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg7", true, true);

			newVersion = new Version(2, 2, 0);

			// Update packageinfo file
			project.setPackageInfo("pkg1", newVersion);
			version = project.getPackageInfo("pkg1");
			assertEquals(newVersion, version);
			checkPackageInfoFiles(project, "pkg1", true, false);

		}
		finally {
			IO.deleteWithException(tmp);
		}
	}

	private static void checkPackageInfoFiles(Project project, String packageName, boolean expectPackageInfo,
			boolean expectPackageInfoJava) throws Exception {
		File pkgInfo = IO.getFile(project.getSrc(), packageName + "/packageinfo");
		File pkgInfoJava = IO.getFile(project.getSrc(), packageName + "/package-info.java");
		assertEquals(expectPackageInfo, pkgInfo.exists());
		assertEquals(expectPackageInfoJava, pkgInfoJava.exists());
	}

	public static void testBuildAll() throws Exception {
		assertTrue(testBuildAll("*", 16).check()); // there are 14 projects
		assertTrue(testBuildAll("p*", 10).check()); // 7 begin with p, plus
													// build-all
		assertTrue(testBuildAll("!p*, *", 6).check()); // negation: 6 don't
														// begin with p,
														// including build-all
		assertTrue(testBuildAll("*-*", 7).check()); // more than one wildcard: 7
													// have a dash
		assertTrue(testBuildAll("!p*, p1, *", 6).check("Missing dependson p1")); // check
																					// that
																					// an
																					// unused
																					// instruction
																					// is
																					// an
																					// error
		assertTrue(testBuildAll("p*, !*-*, *", 13).check()); // check that
																// negation
																// works after
																// some projects
																// have been
																// selected.
	}

	private static Project testBuildAll(String dependsOn, int count) throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project all = ws.getProject("build-all");
		all.setProperty("-dependson", dependsOn);
		all.prepare();
		Collection<Project> dependson = all.getDependson();
		assertEquals(count, dependson.size());
		return all;
	}
	
	public static void testBndPropertiesMacro() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project p = ws.getProject("p7");
		String string = p.getProperty("var", "");
		assertEquals("something;version=latest", string);
	}
}
