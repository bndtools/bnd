package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.launcher.plugin.*;

public class ProjectLaunchImplTest extends TestCase {
	public void testParseRunProperties() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p1");

		ProjectLauncherImpl launcher = new ProjectLauncherImpl(project);
		launcher.prepare();

		String arg = (String) launcher.getRunVM().iterator().next();
		String propertiesPath = arg.substring("-Dlauncher.properties=".length());

		Properties launchProps = new Properties();
		launchProps.load(new FileInputStream(new File(propertiesPath)));
		assertEquals("", launchProps.get("launch.bundles"));
	}
}
