package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.eclipse.EclipseClasspath;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.lib.deployer.FileRepo;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings({
	"resource", "restriction"
})
public class ProjectTest extends TestCase {
	File tmp;

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
	}

	public void testAliasbuild() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("p3");
		project.setProperty("-x-overwritestrategy", "disposable-names");

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

			assertTrue(Files.isSymbolicLink(files[0].toPath()));
			Path linkedTo = Files.readSymbolicLink(files[0].toPath());
			assertTrue(linkedTo.toString()
				.endsWith("-0"));

		} finally {
			project.clean();
		}
	}

	/**
	 * Test require bnd
	 */
	public void testRequireBndFail() throws Exception {
		try (Workspace ws = getWorkspace(IO.getFile("testresources/ws")); Project top = ws.getProject("p1")) {
			top.setProperty("-resourceonly", "true");
			top.setProperty("-includeresource", "a;literal=''");
			top.setProperty("-require-bnd", "\"(version=100000.0)\"");
			top.build();
			assertTrue(top.check("-require-bnd fails for filter \\(version=100000.0\\) values=\\{version=.*\\}"));
		}
	}

	public void testRequireBndPass() throws Exception {
		try (Workspace ws = getWorkspace(IO.getFile("testresources/ws")); Project top = ws.getProject("p1")) {
			top.setProperty("-resourceonly", "true");
			top.setProperty("-includeresource", "a;literal=''");
			top.setProperty("-require-bnd", "\"(version>=" + About.CURRENT + ")\"");
			top.build();
			assertTrue(top.check());
		}
	}

	/**
	 * Test -stalecheck
	 *
	 * @throws Exception
	 */
	public void testStaleChecks() throws Exception {

		Workspace ws = getWorkspace("testresources/ws-stalecheck");
		Project project = ws.getProject("p1");
		File foobar = project.getFile("foo.bar");

		foobar.setLastModified(System.currentTimeMillis() + 100000);
		File f = testStaleCheck(project, "\"foo.bar,bnd.bnd\";newer=\"older\";error=FOO", "FOO");
		assertThat(f).isNull();

		f = testStaleCheck(project, "'foo.bar,bnd.bnd';newer=\"older/,younger/\"",
			"detected stale files : foo.bar,bnd.bnd >");
		assertThat(f).isNotNull();

		f = testStaleCheck(project, "foo.bar;newer=older/;warning=FOO", "FOO");
		assertThat(f).isNotNull();

		if (!IO.isWindows()) {
			f = testStaleCheck(project, "foo.bar;newer=older;command='cp foo.bar older/'");
			try (Jar t = new Jar(f)) {
				assertThat(t.getResource("b/c/foo.txt")).isNotNull();
				assertThat(t.getResource("foo.bar")).isNotNull();
			}
		}

		foobar.setLastModified(0);
		testStaleCheck(project, "foo.bar;newer=older");
		testStaleCheck(project, "foo.bar;newer=older;error=FOO");
		testStaleCheck(project, "foo.bar;newer=older;warning=FOO");

		testStaleCheck(project, "older/;newer=foo.bar", "detected");
		testStaleCheck(project, "older/;newer=foo.bar;error=FOO", "FOO");
		testStaleCheck(project, "older/;newer=foo.bar;warning=FOO", "FOO");
	}

	File testStaleCheck(Project project, String clauses, String... check) throws Exception {
		project.clean();
		project.setProperty("-resourcesonly", "true");
		project.setProperty("-includeresource", "older");
		project.setProperty("-stalecheck", clauses);
		File[] build = project.build();
		assertThat(project.check(check)).isTrue();
		return build != null ? build[0] : null;
	}

	/**
	 * Test linked canonical name
	 */
	public void testCanonicalName() throws Exception {

		Workspace ws = getWorkspace("testresources/ws");
		Project project = ws.getProject("p6");
		project.setProperty("-outputmask", "blabla");

		project.clean();
		// Now we build it.
		File[] files = project.build();
		assertTrue(project.check());
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals("blabla", files[0].getName());
		File f = new File(project.getTarget(), "p6.jar");
		assertTrue(f.isFile());
	}

	/**
	 * Test linked canonical name
	 */
	public void testNoCanonicalName() throws Exception {

		Workspace ws = getWorkspace("testresources/ws");
		Project project = ws.getProject("p6");

		project.clean();
		// Now we build it.
		File[] files = project.build();
		assertTrue(project.check());
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals("p6.jar", files[0].getName());
		File f = new File(project.getTarget(), "p6.jar");
		assertTrue(f.isFile());
		assertFalse(IO.isSymbolicLink(f));
	}

	/**
	 * Test the multi-key support on runbundles/runpath/testpath and buildpath
	 */

	public void testMulti() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("multipath");
		assertNotNull(project);

		List<Container> runbundles = new ArrayList<>(project.getRunbundles());
		assertEquals(3, runbundles.size());
		assertEquals("org.apache.felix.configadmin", runbundles.get(0)
			.getBundleSymbolicName());
		assertEquals("org.apache.felix.ipojo", runbundles.get(1)
			.getBundleSymbolicName());
		assertEquals("osgi.core", runbundles.get(2)
			.getBundleSymbolicName());

		List<Container> runpath = new ArrayList<>(project.getRunpath());
		assertEquals(3, runpath.size());

		List<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(3, buildpath.size());

		List<Container> testpath = new ArrayList<>(project.getTestpath());
		assertEquals(3, testpath.size());
	}

	public void testDecoration() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("multipath");
		project.setProperty("-runbundles+", "org.apache.*;startlevel=10");
		assertNotNull(project);

		List<Container> runbundles = new ArrayList<>(project.getRunbundles());
		assertEquals(3, runbundles.size());
		assertEquals("org.apache.felix.configadmin", runbundles.get(0)
			.getBundleSymbolicName());
		assertEquals("10", runbundles.get(0)
			.getAttributes()
			.get("startlevel"));
		assertEquals("org.apache.felix.ipojo", runbundles.get(1)
			.getBundleSymbolicName());
		assertEquals("10", runbundles.get(1)
			.getAttributes()
			.get("startlevel"));
		assertEquals("osgi.core", runbundles.get(2)
			.getBundleSymbolicName());
		assertThat(runbundles.get(2)
			.getAttributes()
			.get("startlevel")).isNull();

		List<Container> runpath = new ArrayList<>(project.getRunpath());
		assertEquals(3, runpath.size());

		List<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(3, buildpath.size());

		List<Container> testpath = new ArrayList<>(project.getTestpath());
		assertEquals(3, testpath.size());
	}

	public void testRepoFilterBuildPath() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("repofilter");
		assertNotNull(project);
		project.setProperty("-buildpath", "p3; version='[1,2)'; repo=Relea*");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(1, buildpath.size());
		// Without repos filter we would get lowest version, i.e. 1.0.0 from the
		// repo named "Repo".
		assertEquals("1.1.0", buildpath.get(0)
			.getVersion());
	}

	public void testRepoFilterBuildPathMultiple() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("repofilter");

		project.setProperty("-buildpath", "org.apache.felix.configadmin; version=latest; repo=\"Rel*,Repo\"");
		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(1, buildpath.size());
		// Expect 1.2.0 from Release repo; not 1.8.8 from Repo2
		assertEquals("1.2.0", buildpath.get(0)
			.getVersion());
	}

	public void testWildcardBuildPath() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("repofilter");
		assertNotNull(project);
		project.setProperty("-buildpath", "lib*");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		// assertEquals(7, buildpath.size());

		for (int i = 1; i < buildpath.size(); i++) {
			Container c = buildpath.get(i);
			assertEquals(Container.TYPE.REPO, c.getType());
		}
	}

	public void testWildcardBuildPathWithRepoFilter() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("repofilter");
		assertNotNull(project);
		project.setProperty("-buildpath", "*; repo=Relea*");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(2, buildpath.size());

		assertEquals(Container.TYPE.REPO, buildpath.get(0)
			.getType());
		assertEquals("org.apache.felix.configadmin", buildpath.get(0)
			.getBundleSymbolicName());

		assertEquals(Container.TYPE.REPO, buildpath.get(1)
			.getType());
		assertEquals("p3", buildpath.get(1)
			.getBundleSymbolicName());
	}

	/**
	 * Check if a project=version, which is illegal on -runbundles, is actually
	 * reported as an error.
	 *
	 * @throws Exception
	 */
	public void testErrorOnVersionIsProjectInRunbundles() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p1");
		top.setProperty("-runbundles", "p2;version=project,p3;version=latest");
		top.getRunbundles();
		assertTrue(top.check("p2 is specified with version=project on -runbundles"));
	}

	/**
	 * https://github.com/bndtools/bnd/issues/395 Repo macro does not refer to
	 * anything
	 */

	public void testRepoMacro2() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p2");
		top.addClasspath(top.getOutput());

		top.setProperty("a", "${repo;org.apache.felix.configadmin;latest}");
		System.out.println("a= '" + top.getProperty("a") + "'");
		assertThat(top.getProperty("a"))
			.endsWith("org.apache.felix.configadmin/org.apache.felix.configadmin-1.8.8.jar");

		top.setProperty("a", "${repo;IdoNotExist;latest}");
		top.getProperty("a");
		assertTrue(top.check("macro refers to an artifact IdoNotExist-latest.*that has an error"));
		assertThat(top.getProperty("a")).isEmpty();
	}

	/**
	 * Two subsequent builds should not change the last modified if none of the
	 * source inputs have been modified.
	 *
	 * @throws Exception
	 */
	public void testLastModified() throws Exception {

		Workspace ws = getWorkspace("testresources/ws");
		Project project = ws.getProject("p6");
		File bnd = IO.getFile("testresources/ws/p6/bnd.bnd");
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

			byte[] olderDigest;
			try (Jar older = new Jar(files[0])) {
				olderDigest = older.getTimelessDigest();
			}
			System.out.println();
			Thread.sleep(3000); // Ensure system time granularity is < than
								// wait

			files[0].delete();

			project.build();
			assertTrue(project.check());
			assertNotNull(files);
			assertEquals(1, files.length);

			byte[] newerDigest;
			try (Jar newer = new Jar(files[0])) {
				newerDigest = newer.getTimelessDigest();
			}

			assertTrue(Arrays.equals(olderDigest, newerDigest));
		} finally {
			project.clean();
		}
	}

	/**
	 * #194 StackOverflowError when -runbundles in bnd.bnd refers to itself
	 */

	public void testProjectReferringToItself() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("bug194");
		top.setDelayRunDependencies(false);
		top.addClasspath(top.getOutput());
		assertTrue(top.check("Circular dependency context"));
	}

	/**
	 * Test if you can add directories and files to the classpath. Originally
	 * checked only for files
	 */

	public void testAddDirToClasspath() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p1");
		top.addClasspath(top.getOutput());
		assertTrue(top.check());
	}

	/**
	 * Test bnd.bnd of project `foo`: `-runbundles: foo;version=latest`
	 */
	public void testRunBundlesContainsSelf() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p1");
		top.setDelayRunDependencies(false);
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

	public void testSameBsnRunBundles() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
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

	public void testRunbundleDuplicates() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p1");
		top.setPedantic(true);
		top.clear();
		top.setProperty("-runbundles", "org.apache.felix.configadmin,org.apache.felix.configadmin");
		Collection<Container> runbundles = top.getRunbundles();
		assertTrue(top.check("Multiple bundles with the same final URL", "Duplicate name"));
		assertNotNull(runbundles);
		assertEquals(1, runbundles.size());
	}

	/**
	 * Check isStale
	 */

	public void testIsStale() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p-stale");
		assertNotNull(top);
		top.build();
		Project bottom = ws.getProject("p-stale-dep");
		assertNotNull(bottom);
		bottom.build();

		long lastModified = bottom.lastModified();
		top.getPropertiesFile()
			.setLastModified(lastModified + 1000);

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

	private void stale(Project project, boolean b) throws Exception {
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
	public void testMultipleRepos() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("p1");
		project.setPedantic(true);
		System.err.println(project.getBundle("org.apache.felix.configadmin", "1.1.0", Strategy.EXACT, null));
		System.err.println(project.getBundle("org.apache.felix.configadmin", "1.1.0", Strategy.HIGHEST, null));
		System.err.println(project.getBundle("org.apache.felix.configadmin", "1.1.0", Strategy.LOWEST, null));

		List<Container> bundles = project.getBundles(Strategy.LOWEST,
			"org.apache.felix.configadmin;version=1.1.0,org.apache.felix.configadmin;version=1.1.0", "test");
		assertTrue(project.check("Multiple bundles with the same final URL", "Duplicate name"));
		assertEquals(1, bundles.size());
	}

	/**
	 * Check if the getSubBuilders properly predicts the output.
	 */

	public void testSubBuilders() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
		try (Project project = ws.getProject("p4-sub")) {
			try (ProjectBuilder pb = project.getBuilder(null)) {
				List<Builder> bs = pb.getSubBuilders();
				assertNotNull(bs);
				assertEquals(3, bs.size());
				Set<String> names = new HashSet<>();
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
				assertEquals(0, project.getErrors()
					.size());
				assertEquals(0, project.getWarnings()
					.size());
				assertNotNull(files);
				assertEquals(3, files.length);
				for (File file : files) {
					try (Jar jar = new Jar(file)) {
						Manifest m = jar.getManifest();
						assertTrue(names.contains(m.getMainAttributes()
							.getValue("Bundle-SymbolicName")));
					}
				}

				assertEquals(12, project.getExports()
					.size());
				assertEquals(18, project.getImports()
					.size());
				assertEquals(12, project.getContained()
					.size());
			}
		}

	}

	/**
	 * Tests the handling of the -sub facility
	 *
	 * @throws Exception
	 */

	public void testSub() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("p4-sub");
		File[] files = project.build();
		Arrays.sort(files);

		System.err.println(Processor.join(project.getErrors(), "\n"));
		System.err.println(Processor.join(project.getWarnings(), "\n"));

		assertEquals(0, project.getErrors()
			.size());
		assertEquals(0, project.getWarnings()
			.size());
		assertNotNull(files);
		assertEquals(3, files.length);

		try (Jar a = new Jar(files[0]); Jar b = new Jar(files[1])) {
			Manifest ma = a.getManifest();
			Manifest mb = b.getManifest();

			assertEquals("base", ma.getMainAttributes()
				.getValue("Base-Header"));
			assertEquals("base", mb.getMainAttributes()
				.getValue("Base-Header"));
			assertEquals("a", ma.getMainAttributes()
				.getValue("Sub-Header"));
			assertEquals("b", mb.getMainAttributes()
				.getValue("Sub-Header"));
		}
	}

	public void testOutofDate() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("p3");
		File bnd = IO.getFile("testresources/ws/p3/bnd.bnd");
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
		} finally {
			project.clean();
		}
	}

	public void testRepoMacro() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project project = ws.getProject("p2");
		System.err.println(project.getPlugins(FileRepo.class));
		String s = project.getReplacer()
			.process(("${repo;libtest}"));
		System.err.println(s);
		assertThat(s).contains("org.apache.felix.configadmin/org.apache.felix.configadmin-1.8.8",
			"org.apache.felix.ipojo/org.apache.felix.ipojo-1.0.0.jar");

		s = project.getReplacer()
			.process(("${repo;libtestxyz}"));
		assertThat(s).matches("");

		s = project.getReplacer()
			.process("${repo;org.apache.felix.configadmin;1.0.0;highest}");
		assertThat(s).endsWith("org.apache.felix.configadmin-1.8.8.jar");
		s = project.getReplacer()
			.process("${repo;org.apache.felix.configadmin;1.0.0;lowest}");
		assertThat(s).endsWith("org.apache.felix.configadmin-1.0.1.jar");
	}

	public void testClasspath() throws Exception {
		File project = new File("").getAbsoluteFile();
		File workspace = project.getParentFile();
		Processor processor = new Processor();
		EclipseClasspath p = new EclipseClasspath(processor, workspace, project);
		System.err.println(p.getDependents());
		System.err.println(p.getClasspath());
		System.err.println(p.getSourcepath());
		System.err.println(p.getOutput());
	}

	public void testBump() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
		Project project = ws.getProject("p1");
		int size = project.getProperties()
			.size();
		Version old = new Version(project.getProperty("Bundle-Version"));
		System.err.println("Old version " + old);
		project.bump("=+0");
		Version newv = new Version(project.getProperty("Bundle-Version"));
		System.err.println("New version " + newv);
		assertEquals(old.getMajor(), newv.getMajor());
		assertEquals(old.getMinor() + 1, newv.getMinor());
		assertEquals(0, newv.getMicro());
		assertEquals(size, project.getProperties()
			.size());
		assertEquals("sometime", newv.getQualifier());
	}

	public void testBumpIncludeFile() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
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

	public void testBumpSubBuilders() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
		Project project = ws.getProject("bump-sub");
		project.setTrace(true);

		assertNull(project.getProperty("Bundle-Version"));

		project.bump("=+0");

		assertNull(project.getProperty("Bundle-Version"));

		try (ProjectBuilder pb = project.getBuilder(null)) {
			for (Builder b : pb.getSubBuilders()) {
				assertEquals(new Version(1, 1, 0), new Version(b.getVersion()));
			}
		}
	}

	public void testRunBuilds() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));

		// Running a .bnd includes built bundles by default
		Project p1 = ws.getProject("p1");
		assertTrue(p1.getRunBuilds());

		// Can override the default by specifying -runbuilds: false
		Project p2 = ws.getProject("p2");
		assertFalse(p2.getRunBuilds());

		// Running a .bndrun DOES NOT include built bundles by default
		Project p1a = new Project(ws, IO.getFile("testresources/ws/p1"), IO.getFile("testresources/ws/p1/p1a.bndrun"));
		assertFalse(p1a.getRunBuilds());

		// ... unless we override the default by specifying -runbuilds: true
		Project p1b = new Project(ws, IO.getFile("testresources/ws/p1"), IO.getFile("testresources/ws/p1/p1b.bndrun"));
		assertTrue(p1b.getRunBuilds());
	}

	public void testSetPackageVersion() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
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

	/*
	 * Verify that this also works when you have multiple directories
	 */
	public void testMultidirsrc() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
		Project p = ws.getProject("pmuldirsrc");
		Collection<File> sourcePath = p.getSourcePath();
		assertEquals(2, sourcePath.size());
		assertTrue(sourcePath.contains(p.getFile("a")));
		assertTrue(sourcePath.contains(p.getFile("b")));

		//
		// pkgb = in b
		//

		Version version = new Version("2.0.0");
		p.setPackageInfo("pkgb", version);
		Version newer = p.getPackageInfo("pkgb");
		assertEquals(version, newer);

		assertFalse(p.getFile("a/pkgb/package-info.java")
			.isFile());
		assertFalse(p.getFile("a/pkgb/packageinfo")
			.isFile());
		assertFalse(p.getFile("b/pkgb/package-info.java")
			.isFile());
		assertTrue(p.getFile("b/pkgb/packageinfo")
			.isFile());
	}

	/*
	 * Verify that this also works when you have multiple directories
	 */
	public void testSrcPointsToFile() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
		Project p = ws.getProject("pfilesrc");
		Collection<File> sourcePath = p.getSourcePath();
		File a = p.getFile("a");
		File b = p.getFile("b");
		assertThat(sourcePath).containsExactly(a);
		assertThat(p.check("src.*" + Pattern.quote(b.getName()))).isTrue();
	}

	/*
	 * Verify that that -versionannotations works. We can be osgi, bnd,
	 * packageinfo, or an annotation. When not set, we are packageinfo
	 */
	public void testPackageInfoType() throws Exception {
		Workspace ws = getWorkspace("testresources/ws");
		Project project = ws.getProject("p5");
		project.setTrace(true);

		Version newVersion = new Version(2, 0, 0);

		project.setProperty(Constants.PACKAGEINFOTYPE, "bnd");
		// Package with no package info

		project.setPackageInfo("pkg1", newVersion);
		Version version = project.getPackageInfo("pkg1");
		assertEquals(newVersion, version);
		checkPackageInfoFiles(project, "pkg1", false, true);
		String content = IO.collect(project.getFile("src/pkg1/package-info.java"));
		assertTrue(content.contains("import aQute.bnd.annotation.Version"));

		// Package with package-info.java containing @Version("1.0.0")
		project.setPackageInfo("pkg2", newVersion);
		version = project.getPackageInfo("pkg2");
		assertEquals(newVersion, version);
		checkPackageInfoFiles(project, "pkg2", false, true);

		// new packageinfo must now contain osgi ann.
		project.setProperty(Constants.PACKAGEINFOTYPE, "osgi");

		// Package with package-info.java containing version + packageinfo
		project.setPackageInfo("pkg5", newVersion);
		version = project.getPackageInfo("pkg5");
		assertEquals(newVersion, version);
		checkPackageInfoFiles(project, "pkg5", true, true);
		content = IO.collect(project.getFile("src/pkg5/package-info.java"));
		assertTrue(content.contains("import aQute.bnd.annotation.Version"));

		// Package with package-info.java NOT containing version +
		// packageinfo
		project.setPackageInfo("pkg6", newVersion);
		version = project.getPackageInfo("pkg6");
		assertEquals(newVersion, version);
		checkPackageInfoFiles(project, "pkg6", true, true);
		content = IO.collect(project.getFile("src/pkg6/package-info.java"));
		assertTrue(content.contains("import org.osgi.annotation.versioning.Version"));

		// Package with package-info.java NOT containing version
		project.setPackageInfo("pkg7", newVersion);
		version = project.getPackageInfo("pkg7");
		assertEquals(newVersion, version);
		checkPackageInfoFiles(project, "pkg7", false, true);

		newVersion = new Version(2, 2, 0);

		// Update packageinfo file
		project.setPackageInfo("pkg1", newVersion);
		version = project.getPackageInfo("pkg1");
		assertEquals(newVersion, version);
		checkPackageInfoFiles(project, "pkg1", false, true);
	}

	private void checkPackageInfoFiles(Project project, String packageName, boolean expectPackageInfo,
		boolean expectPackageInfoJava) throws Exception {

		File pkgInfo = project.getFile("src/" + packageName.replace('.', '/') + "/packageinfo");
		File pkgInfoJava = project.getFile("src/" + packageName.replace('.', '/') + "/package-info.java");
		assertEquals(expectPackageInfo, pkgInfo.exists());
		assertEquals(expectPackageInfoJava, pkgInfoJava.exists());
	}

	public void testBuildAll() throws Exception {
		assertTrue(testBuildAll("*", 20).check()); // there are 20 projects
		assertTrue(testBuildAll("p*", 12).check()); // 12 begin with p
		assertTrue(testBuildAll("!p*, *", 8).check()); // negation: 6 don't
														// begin with p
		assertTrue(testBuildAll("*-*", 6).check()); // more than one wildcard: 7
													// have a dash
		assertTrue(testBuildAll("!p*, p1, *", 8).check("Missing dependson p1")); // check
																					// that
																					// an
																					// unused
																					// instruction
																					// is
																					// an
																					// error
		assertTrue(testBuildAll("p*, !*-*, *", 18).check()); // check that
																// negation
																// works after
																// some projects
																// have been
																// selected.
	}

	/**
	 * Check that the output property can be used to name the output binary.
	 */
	public void testGetOutputFile() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws"));
		Project top = ws.getProject("p1");

		//
		// We expect p1 to be a single project (no sub builders)
		//
		try (ProjectBuilder pb = top.getBuilder(null)) {
			assertEquals("p1 must be singleton", 1, pb.getSubBuilders()
				.size());
			Builder builder = pb.getSubBuilders()
				.get(0);
			assertEquals("p1 must be singleton", "p1", builder.getBsn());

			// Check the default bsn.jar form

			assertEquals(new File(top.getTarget(), "p1.jar"), top.getOutputFile("p1"));
			assertEquals(new File(top.getTarget(), "p1.jar"), top.getOutputFile("p1", "0"));

			// Add the version to the filename
			top.setProperty("-outputmask", "${@bsn}-${version;===s;${@version}}.jar");
			assertEquals(new File(top.getTarget(), "p1-1.260.0.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));

			top.setProperty("Bundle-Version", "1.260.0.SNAPSHOT");
			assertEquals(new File(top.getTarget(), "p1-1.260.0-SNAPSHOT.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));

			top.setProperty("-outputmask", "${@bsn}-${version;===S;${@version}}.jar");
			assertEquals(new File(top.getTarget(), "p1-1.260.0-SNAPSHOT.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));

			top.setProperty("Bundle-Version", "1.260.0.NOTSNAPSHOT");
			top.setProperty("-outputmask", "${@bsn}-${version;===S;${@version}}.jar");
			assertEquals(new File(top.getTarget(), "p1-1.260.0.NOTSNAPSHOT.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));

			top.setProperty("-outputmask", "${@bsn}-${version;===s;${@version}}.jar");
			assertEquals(new File(top.getTarget(), "p1-1.260.0.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));

			top.setProperty("Bundle-Version", "42");
			top.setProperty("-outputmask", "${@bsn}-${version;===S;${@version}}.jar");
			assertEquals(new File(top.getTarget(), "p1-42.0.0.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));

			top.setProperty("-outputmask", "${@bsn}-${version;===s;${@version}}.jar");
			assertEquals(new File(top.getTarget(), "p1-42.0.0.jar"),
				top.getOutputFile(builder.getBsn(), builder.getVersion()));
		}
	}

	private Workspace getWorkspace(File file) throws Exception {
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	private Workspace getWorkspace(String dir) throws Exception {
		return getWorkspace(new File(dir));
	}

	private Project testBuildAll(String dependsOn, int count) throws Exception {
		Workspace ws = new Workspace(IO.getFile("testresources/ws"));
		Project all = ws.getProject("build-all");
		all.setProperty("-dependson", dependsOn);
		all.prepare();
		Collection<Project> dependson = all.getDependson();
		assertEquals(count, dependson.size());
		return all;
	}

	public static void testVmArgs() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project p = ws.getProject("p7");
		Collection<String> c = p.getRunVM();

		String[] arr = c.toArray(new String[0]);
		assertEquals("-XX:+UnlockCommercialFeatures", arr[0]);
		assertEquals("-XX:+FlightRecorder", arr[1]);
		assertEquals("-XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true", arr[2]);
	}

	public void testHashVersion() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws-versionhash"));
		Project project = ws.getProject("p1");
		assertNotNull(project);
		project.setProperty("-buildpath",
			"tmp; version=hash; hash=7fe83bfd5999fa4ef9cec40282d5d67dd0ff3303bac6b8c7b0e8be80a821441c");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(1, buildpath.size());
		assertEquals("bar", buildpath.get(0)
			.getManifest()
			.getMainAttributes()
			.getValue("Prints"));
	}

	public void testHashVersionWithAlgorithm() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws-versionhash"));
		Project project = ws.getProject("p1");
		assertNotNull(project);
		project.setProperty("-buildpath",
			"tmp; version=hash; hash=SHA-256:7fe83bfd5999fa4ef9cec40282d5d67dd0ff3303bac6b8c7b0e8be80a821441c");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(1, buildpath.size());
		assertEquals("bar", buildpath.get(0)
			.getManifest()
			.getMainAttributes()
			.getValue("Prints"));
	}

	public void testHashVersionWithAlgorithmNotFound() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws-versionhash"));
		Project project = ws.getProject("p1");
		assertNotNull(project);
		project.setProperty("-buildpath",
			"tmp; version=hash; hash=SHA-1:7fe83bfd5999fa4ef9cec40282d5d67dd0ff3303bac6b8c7b0e8be80a821441c");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(0, buildpath.size());
	}

	public void testHashVersionNonMatchingBsn() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws-versionhash"));
		Project project = ws.getProject("p1");
		assertNotNull(project);
		project.setProperty("-buildpath",
			"WRONG; version=hash; hash=SHA-256:7fe83bfd5999fa4ef9cec40282d5d67dd0ff3303bac6b8c7b0e8be80a821441c");

		ArrayList<Container> buildpath = new ArrayList<>(project.getBuildpath());
		assertEquals(0, buildpath.size());
	}

	public void testCopyRepo() throws Exception {
		Workspace ws = getWorkspace(IO.getFile("testresources/ws-repo-test"));
		Project project = ws.getProject("p1");
		assertNotNull(ws);
		assertNotNull(project);

		RepositoryPlugin repo = ws.getRepository("Repo");
		assertNotNull(repo);
		RepositoryPlugin release = ws.getRepository("Release");
		assertNotNull(release);

		project.copy(repo, (String) null, release);
		assertTrue(project.check());
		assertTrue(ws.check());
	}
}
