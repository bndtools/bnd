package test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class LauncherTest extends TestCase {

	private static Workspace	workspace;
	private static Project		project;

	public void tearDown() throws IOException {
		if (project != null) {
			project.close();
			workspace.close();
		}
	}

	/**
	 * Test the packager for remote
	 * 
	 * @throws Exception
	 */
	public static void testRemotePackager() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-runpath", "biz.aQute.remote.launcher;version=latest");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		Jar executable = l.executable();
		assertTrue(project.check());
		assertNotNull(executable);
	}

	/**
	 * Try out the new tester that does not contain JUnit
	 */
	public static void testJUnitLessTester() throws Exception {
		Project project = getProject();

		List<Container> bundles = project.getBundles(Strategy.HIGHEST, "biz.aQute.tester", "TESTER");
		assertNotNull(bundles);
		assertEquals(1, bundles.size());

		project.setProperty(Constants.TESTPATH, "");
		project.setProperty(Constants.TESTER, "biz.aQute.tester");
		project.clear();
		project.build();
		assertTrue(project.check());

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
		pt.addTest("test.TestCase2:m1");

		assertEquals(2, pt.test());
	}

	/**
	 * Gradle Problems exporting an executable jar #980 Test the packager's
	 * difference between plan export in gradle & from bndtools
	 * 
	 * @throws Exception
	 */
	public static void testPackagerDifference() throws Exception {

		//
		// First as we basically do it in bndtools for a project
		//

		{
			Project project = getProject();
			project.clear();
			ProjectLauncher l = project.getProjectLauncher();
			l.setTrace(true);
			Jar executable = l.executable();
			assertNotNull(executable);

			Properties p = new Properties();
			Resource resource = executable.getResource("launcher.properties");

			try (InputStream in = resource.openInputStream()) {
				p.load(in);
			}

			assertEquals("workspace", p.getProperty("in.workspace"));
			assertEquals("project", p.getProperty("in.project"));
			assertEquals("project", p.getProperty("in.bndrun"));
		}

		//
		// First as we basically do it in bndtools for a file
		//

		{
			Project project = getProject();
			project.clear();
			File f = project.getFile("x.bndrun");
			try (Run run = new Run(project.getWorkspace(), project.getBase(), f);) {

				ProjectLauncher l = run.getProjectLauncher();
				l.setTrace(true);
				Jar executable = l.executable();
				assertNotNull(executable);

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertEquals("workspace", p.getProperty("in.workspace"));
				assertEquals("workspace", p.getProperty("in.project"));
				assertEquals("bndrun", p.getProperty("in.bndrun"));
			}
		}

		// Test project with export

		{

			Project project = getProject();
			project.clear();
			File f = new File("generated/test.jar");
			project.export(null, false, f);

			try (Jar executable = new Jar(f);) {

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertEquals("workspace", p.getProperty("in.workspace"));
				assertEquals("project", p.getProperty("in.project"));
				assertEquals("project", p.getProperty("in.bndrun"));
			}
		}

		// Test file with export

		{

			Project project = getProject();
			project.clear();
			File f = new File("generated/test.jar");
			project.export("x.bndrun", false, f);

			try (Jar executable = new Jar(f);) {

				Properties p = new Properties();
				Resource resource = executable.getResource("launcher.properties");

				try (InputStream in = resource.openInputStream()) {
					p.load(in);
				}

				assertEquals("workspace", p.getProperty("in.workspace"));
				assertEquals("workspace", p.getProperty("in.project"));
				assertEquals("bndrun", p.getProperty("in.bndrun"));
			}
		}

	}

	/**
	 * junit 4 "unrooted" tests with parametrized tests #661
	 * 
	 * @throws Exception
	 */
	public static void testJunit4Tester() throws Exception {
		Project project = getProject();
		project.clear();
		project.build();

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.Junit4TestCase");

		assertEquals(0, pt.test());
		assertTrue(project.check());
	}

	// public static void testLocalLaunch() throws Exception {
	// Project project = getProject();
	// ProjectLauncher l = project.getProjectLauncher();
	// l.setTrace(true);
	// l.getRunProperties().put("test.cmd", "exit");
	// //assertTrue(project.check());
	// assertEquals(42, l.start(null));
	// }

	/**
	 * Test if we can keep the framework state.
	 */
	public static void testRunKeep() throws Exception {

		//
		// First set persistence after clearing the storage
		//
		Project project = getProject();
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		ProjectLauncher l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "setpersistence");
		assertEquals(55, l.launch());

		//
		// Check that we really clear by clearing and checking state
		// this must fail with -2
		//

		project = getProject();
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "getpersistence");
		assertEquals(-2, l.launch());

		//
		// We now try to set the state again with a cleared framework
		//
		project = getProject();
		project.setProperty("-runkeep", "false");
		project.setProperty("-runstorage", "generated/keepfw");
		l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "setpersistence");
		assertEquals(55, l.launch());

		//
		// And now it should have been saved if we do not clear
		// the framework
		//

		project = getProject();
		project.setProperty("-runkeep", "true");
		project.setProperty("-runstorage", "generated/keepfw");
		l = project.getProjectLauncher();

		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "getpersistence");
		assertEquals(65, l.launch());

	}

	public static void testNoReferences() throws Exception {
		Project project = getProject();
		project.setProperty("-runnoreferences", true + "");

		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "noreference");
		assertEquals(15, l.launch());
	}

	/**
	 * Try launching a workspace with spaces
	 */
	public static void testSpaces() throws Exception {
		File f = new File("t m p");
		try {
			File cnf = new File(f, "cnf");
			File demo = new File(f, "demo");
			IO.copy(IO.getFile("../cnf"), IO.getFile(f, "cnf"));
			IO.copy(IO.getFile("../demo"), IO.getFile(f, "demo"));
			IO.copy(IO.getFile("../biz.aQute.launcher"), IO.getFile(f, "biz.aQute.launcher"));
			IO.copy(IO.getFile("../biz.aQute.junit"), IO.getFile(f, "biz.aQute.junit"));

			Workspace ws = Workspace.getWorkspace(f);
			Project p = ws.getProject("demo");
			p.setTrace(true);
			p.build();
			try {
				ProjectLauncher l = p.getProjectLauncher();
				l.setTrace(true);
				l.getRunProperties().put("test.cmd", "exit");
				assertEquals(42, l.launch());
			} finally {
				p.close();
				ws.close();
			}
		} finally {
			IO.delete(f);
		}
	}

	/**
	 * Test the java agent
	 * 
	 * @throws Exception
	 */
	public static void testAgent() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-javaagent", "true");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "agent");
		assertEquals(55, l.launch());
	}

	/**
	 * Test env variables
	 * 
	 * @throws Exception
	 */
	public static void testEnv() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-runenv", "ANSWER=84");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "env");
		assertEquals(84, l.launch());
	}

	/**
	 * Tests if the properties are cleaned up. This requires some knowledge of
	 * the launcher unfortunately. It is also not sure if the file is not just
	 * deleted by the onExit ...
	 * 
	 * @throws Exception
	 */
	public static void testCleanup() throws Exception {
		Project project = getProject();
		File target = project.getTarget();
		IO.deleteWithException(target);
		project.clear();
		assertNoProperties(target);
		final ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					l.cancel();
				} catch (Exception e) {
					// Ignore
				}
			}
		};

		t.start();
		l.getRunProperties().put("test.cmd", "timeout");
		l.launch();
		assertNoProperties(target);
	}

	/**
	 * The properties file is an implementation detail ... so this is white box
	 * testing.
	 * 
	 * @param project
	 * @throws Exception
	 */
	private static void assertNoProperties(File target) throws Exception {
		if (!target.exists())
			return;

		for (File file : target.listFiles()) {
			if (file.getAbsolutePath().startsWith("launch")) {
				fail("There is a launch file in the target directory: " + file);
			}
		}
	}

	public static void testSimple() throws Exception {
		Project project = getProject();
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "exit");
		// assertTrue(project.check());
		assertEquals(42, l.launch());
	}

	/**
	 * Test the packager
	 * 
	 * @throws Exception
	 */
	public static void testPackager() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("[debug]testprop", "debug");
		project.setProperty("[exec]testprop", "exec");
		project.setProperty("Header", "${testprop}");
		project.setProperty(Constants.PROFILE, "exec");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		Jar executable = l.executable();
		assertNotNull(executable);
		assertEquals("exec", project.getProperty("testprop"));
		assertEquals("exec", project.getProperty("Header"));
	}

	/**
	 * This needs to be adapted because the previous left lots of files after
	 * testing. This current one does not work since the demo project uses the
	 * snapshots of the launcher and tester, and when copied they are not there
	 * in that workspace. So we need another demo project that does not use OSGi
	 * and has not special deps. Then the following code can be used.
	 * 
	 * @throws Exception
	 */
	// public static void testWorkspaceWithSpace() throws Exception {
		// // reuse built .class files from the demo project.
		// String base = new
		// File("").getAbsoluteFile().getParentFile().getAbsolutePath();
		// File ws = IO.getFile("tmp/ space ");
		// try {
		// IO.delete(ws);
		// ws.mkdirs();
		// IO.copy( IO.getFile("../demo"), IO.getFile(ws, "demo"));
		// IO.getFile(ws, "cnf").mkdirs();
		// IO.copy( IO.getFile("../cnf"), IO.getFile(ws, "cnf"));
		// Workspace wp = new Workspace(ws);
		//
		// Project p = wp.getProject("demo");
		// p.clear();
		// ProjectLauncher l = p.getProjectLauncher();
		// l.setTrace(true);
		// l.getRunProperties().put("test.cmd", "exit");
		// assertEquals(42, l.launch());
		// }
		// finally {
		// IO.delete(ws);
		// }
	// }

	/**
	 * @throws Exception
	 */
	static Project getProject() throws Exception {
		workspace = Workspace.getWorkspace(new File("").getAbsoluteFile().getParentFile());
		workspace.clear();
		project = workspace.getProject("demo");
		project.setTrace(true);
		project.clear();
		project.forceRefresh();
		assertTrue(project.check());
		// (project.getWorkspace().check());
		return project;
	}

	public static void testTester() throws Exception {
		Project project = getProject();
		project.clear();
		project.build();

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");
		pt.addTest("test.TestCase2:m1");

		assertEquals(2, pt.test());
	}

	public static void testTimeoutActivator() throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(100, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		assertEquals(ProjectLauncher.TIMEDOUT, l.launch());

	}

	public static void testTimeout() throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(100, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		l.getRunProperties().put("test.cmd", "timeout");
		assertEquals(ProjectLauncher.TIMEDOUT, l.launch());
	}

	/**
	 * Allowing Runnable and Callable, with Callable as priority
	 * 
	 * @throws Exception
	 */
	public static void testMainThread() throws Exception {
		assertExitCode("main.thread", ProjectLauncher.OK);
	}

	public static void testMainThreadBoth() throws Exception {
		assertExitCode("main.thread.both", 43);
	}

	public static void testMainThreadCallableNull() throws Exception {
		assertExitCode("main.thread.callablenull", 0);
	}

	public static void testMainThreadInvalidType() throws Exception {
		assertExitCode("main.thread.callableinvalidtype", 0);
	}

	public static void testMainThreadCallable() throws Exception {
		assertExitCode("main.thread.callable", 42);
	}

	public static void testFrameworkStop() throws Exception {
		assertExitCode("framework.stop", -9);
	}

	private static void assertExitCode(String cmd, int rv) throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(15000, TimeUnit.MILLISECONDS);
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", cmd);
		assertEquals(rv, l.launch());
	}

	public void testUnresolved() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty(Constants.RUNTRACE, "true");

		String mandatorynoversion = IO.getFile("jar/mandatorynoversion.jar").getAbsolutePath();
		String runbundles = project.getProperty(Constants.RUNBUNDLES);
		project.setProperty(Constants.RUNBUNDLES, runbundles + "," + mandatorynoversion + ";version=file");
		ProjectTester tester = project.getProjectTester();
		tester.prepare();
		ProjectLauncher l = tester.getProjectLauncher();
		l.addRunBundle(mandatorynoversion);
		l.setTimeout(25000, TimeUnit.MILLISECONDS);
		l.setTrace(true);
		assertEquals(1, l.launch());
	}
}
