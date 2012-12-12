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

		ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
		launcher.prepare();

		String arg = launcher.getRunVM().iterator().next();
		String propertiesPath = arg.substring("-Dlauncher.properties=".length());

		Properties launchProps = new Properties();
		launchProps.load(new FileInputStream(new File(propertiesPath)));
		assertEquals(new File("src/test/ws/p1/generated/p1.jar").getAbsolutePath(), launchProps.get("launch.bundles"));
	}
	
	public static void testParseSystemCapabilities() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("src/test/ws"));
		Project project = ws.getProject("p1");

		ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
		launcher.prepare();
		
		Map<String, ? extends Map<String,String>> caps = launcher.getSystemCapabilities();
		assertEquals(1, caps.size());
		Map<String,String> entryMap = caps.get("osgi.native");
		
		assertEquals(2, entryMap.size());
		String osnames = entryMap.get("osgi.native.osname");
		assertEquals("Win7,Windows7,Windows 7", osnames);
	}
}
