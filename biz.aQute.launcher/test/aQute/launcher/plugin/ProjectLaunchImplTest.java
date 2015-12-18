package aQute.launcher.plugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ProjectLaunchImplTest extends TestCase {

	private Workspace ws;

	protected void setUp() throws Exception {
		ws = new Workspace(IO.getFile("test/ws"));
	}

	protected void tearDown() throws Exception {
		for (Project project : ws.getAllProjects()) {
			project.clean();
		}
		IO.delete(ws.getFile("cnf/cache"));
	}

	public void testParseSystemCapabilities() throws Exception {
		Project project = ws.getProject("p1");
		project.prepare();
		String systemCaps = null;

		try {
			ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
			launcher.prepare();

			systemCaps = launcher.getSystemCapabilities();
			launcher.close();
		} finally {
			project.close();
			ws.close();
		}
		assertEquals(
				"osgi.native;osgi.native.osname:List<String>=\"Win7,Windows7,Windows 7\";osgi.native.osversion:Version=\"6.1\"",
				systemCaps);
	}

	public void testCwdIsProjectBase() throws Exception {
		Project project = ws.getProject("p1");
		project.prepare();
		ProjectLauncherImpl projectLauncherImpl = new ProjectLauncherImpl(project);
		assertEquals(project.getBase(), projectLauncherImpl.getCwd());
		projectLauncherImpl.close();
	}
}
