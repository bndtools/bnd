package aQute.launcher.plugin;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.launcher.constants.*;

public class ProjectLaunchImplTest extends TestCase {
	public static void testParseRunProperties() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p1");
		String lb = null;

		try {
			ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
			launcher.prepare();

			String arg = launcher.getRunVM().iterator().next();
		String s = "-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=";
		String propertiesPath = arg.substring(s.length());
		Matcher matcher = Pattern.compile("^([\"'])(.*)\\1$").matcher(propertiesPath);
		if (matcher.matches()) {
			propertiesPath = matcher.group(2);
		}

			Properties launchProps = new Properties();
			launchProps.load(new FileInputStream(new File(propertiesPath)));
			lb = (String) launchProps.get("launch.bundles");
		}
		finally {
			project.close();
			ws.close();
		}
		assertEquals(new File("test/ws/p1/generated/p1.jar").getAbsolutePath(), lb);
	}

	public static void testParseSystemCapabilities() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
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
