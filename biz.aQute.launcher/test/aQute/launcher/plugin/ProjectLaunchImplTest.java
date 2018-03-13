package aQute.launcher.plugin;

import java.io.File;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ProjectLaunchImplTest extends TestCase {

	private Workspace	ws;
	private File		tmp;

	protected void setUp() throws Exception {
		tmp = new File("generated/tmp/test/" + getName());
		tmp.mkdirs();
		IO.copy(IO.getFile("testresources/ws"), tmp);
		ws = new Workspace(tmp);
	}

	protected void tearDown() throws Exception {
		ws.close();
		IO.delete(tmp);
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
