package aQute.launcher.plugin;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.lib.io.*;

public class ProjectLaunchImplTest extends TestCase {

	private static void reallyClean(Workspace ws) throws Exception {
		for (Project project : ws.getAllProjects()) {
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
		IO.delete(ws.getFile("cnf/cache"));
	}

	public void tearDown() throws Exception {
		reallyClean(new Workspace(new File("test/ws")));
	}

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
