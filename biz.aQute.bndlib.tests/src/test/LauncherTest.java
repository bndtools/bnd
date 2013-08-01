package test;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

public class LauncherTest extends TestCase {

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
			p.build();
			ProjectLauncher l = p.getProjectLauncher();
			l.setTrace(true);
			l.getRunProperties().put("test.cmd", "exit");
			assertEquals(42, l.launch());
		}
		finally {
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
				}
				catch (Exception e) {
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
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "exit");
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
	 * Test the sha packager
	 * 
	 * @throws Exception
	 */
	public static void testShaPackager() throws Exception {
		Project project = getProject();
		project.clear();
		project.setProperty("-package", "jpm");
		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		Jar executable = l.executable();
		assertNotNull(executable);
		Manifest m = executable.getManifest();
		m.write(System.out);
		System.out.flush();
		assertNotNull(m.getMainAttributes().getValue("JPM-Classpath"));
		assertNotNull(m.getMainAttributes().getValue("JPM-Runbundles"));

		Resource r = executable.getResource("launcher.properties");
		assertNotNull(r);

		Properties p = new Properties();
		p.load(r.openInputStream());

		System.out.println(p);

		String s = p.getProperty("launch.bundles");
		assertTrue(s.contains("${JPMREPO}/"));
		assertEquals("false", p.getProperty("launch.embedded"));
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
	public static void testWorkspaceWithSpace() throws Exception {
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
	}

	/**
	 * @return
	 * @throws Exception
	 */
	static Project getProject() throws Exception {
		Workspace workspace = Workspace.getWorkspace(new File("").getAbsoluteFile().getParentFile());
		Project project = workspace.getProject("demo");
		project.clear();
		project.forceRefresh();
		return project;
	}

	static Project getProjectFromWorkspaceWithSpace() throws Exception {
		Workspace workspace = Workspace.getWorkspace(new File("testresources/a space"));
		Project project = workspace.getProject("test");
		return project;
	}

	public static void testTester() throws Exception {
		Project project = getProject();
		project.clear();
		project.build();

		ProjectTester pt = project.getProjectTester();
		pt.addTest("test.TestCase1");

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

	private static void assertExitCode(String cmd, int rv) throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(5000, TimeUnit.MILLISECONDS);
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", cmd);
		assertEquals(rv, l.launch());
	}
}
