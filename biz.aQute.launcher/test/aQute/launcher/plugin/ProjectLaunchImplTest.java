package aQute.launcher.plugin;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;

public class ProjectLaunchImplTest extends TestCase {

	public static void testParseSystemCapabilities() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p1");
		String systemCaps = null;

		try {
			ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
			launcher.prepare();

			systemCaps = launcher.getSystemCapabilities();
		}
		finally {
			project.close();
			ws.close();
		}
		assertEquals(
				"osgi.native;osgi.native.osname:List<String>=\"Win7,Windows7,Windows 7\";osgi.native.osversion:Version=6.1",
				systemCaps);
	}
}
