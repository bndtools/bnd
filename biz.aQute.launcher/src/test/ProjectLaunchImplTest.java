package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.launcher.plugin.*;

public class ProjectLaunchImplTest extends TestCase {
	public static void testParseRunProperties() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("src/test/ws"));
		Project project = ws.getProject("p1");
		String lb = null;

		try {
			ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
			launcher.prepare();

			String arg = launcher.getRunVM().iterator().next();
			String propertiesPath = arg.substring("-Dlauncher.properties=".length());

			Properties launchProps = new Properties();
			launchProps.load(new FileInputStream(new File(propertiesPath)));
			lb = (String) launchProps.get("launch.bundles");
		}
		finally {
			project.close();
			ws.close();
		}
		assertEquals(new File("src/test/ws/p1/generated/p1.jar").getAbsolutePath(), lb);
	}

	public static void testParseSystemCapabilities() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("src/test/ws"));
		Project project = ws.getProject("p1");
		String systemCaps =null;
		
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
