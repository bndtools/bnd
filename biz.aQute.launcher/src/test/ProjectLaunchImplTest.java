package test;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.launcher.constants.*;
import aQute.launcher.plugin.*;

public class ProjectLaunchImplTest extends TestCase {
	public static void testParseRunProperties() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("src/test/ws"));
		Project project = ws.getProject("p1");

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
		assertEquals(new File("src/test/ws/p1/generated/p1.jar").getAbsolutePath(), launchProps.get("launch.bundles"));
	}
	
	public static void testParseSystemCapabilities() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("src/test/ws"));
		Project project = ws.getProject("p1");

		ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
		launcher.prepare();
		
		String systemCaps = launcher.getSystemCapabilities();
		assertEquals("osgi.native;osgi.native.osname:List<String>=\"Win7,Windows7,Windows 7\";osgi.native.osversion:Version=6.1", systemCaps);
	}
}
