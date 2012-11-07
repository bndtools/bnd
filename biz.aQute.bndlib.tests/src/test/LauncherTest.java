package test;

import java.io.*;
import java.util.concurrent.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.lib.io.*;

public class LauncherTest extends TestCase {

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

	public static void testWorkspaceWithSpace() throws Exception {
		Project project = getProjectFromWorkspaceWithSpace();
		
		project.clear();
		project.clean();
		
		// reuse built .class files from the demo project.
		String base = new File("").getAbsoluteFile().getParentFile().getAbsolutePath();
		
		new File(base + "/biz.aQute.bndlib.tests/test/a space/test/bin/test/").mkdirs();
		
		IO.copy(new File(base + "/demo/bin/test/TestActivator$1.class"),
				new File(base + "/biz.aQute.bndlib.tests/test/a space/test/bin/test/TestActivator$1.class"));
		IO.copy(new File(base + "/demo/bin/test/TestActivator.class"),
				new File(base + "/biz.aQute.bndlib.tests/test/a space/test/bin/test/TestActivator.class"));
		
		project.clear();
		project.build();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTrace(true);
		l.getRunProperties().put("test.cmd", "exit");
		assertEquals(42, l.launch());
	}
	
	/**
	 * @return
	 * @throws Exception
	 */
	static Project getProject() throws Exception {
		Workspace workspace = Workspace.getWorkspace(new File("").getAbsoluteFile().getParentFile());
		Project project = workspace.getProject("demo");
		return project;
	}
	
	static Project getProjectFromWorkspaceWithSpace() throws Exception {
		Workspace workspace = Workspace.getWorkspace(new File("test/a space"));
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

	public static void testMainThread() throws Exception {
		Project project = getProject();
		project.clear();

		ProjectLauncher l = project.getProjectLauncher();
		l.setTimeout(10000, TimeUnit.MILLISECONDS);
		l.setTrace(false);
		l.getRunProperties().put("test.cmd", "main.thread");
		assertEquals(ProjectLauncher.OK, l.launch());
	}
}
