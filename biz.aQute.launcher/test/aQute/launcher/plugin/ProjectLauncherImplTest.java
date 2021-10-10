package aQute.launcher.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.lib.io.IO;

public class ProjectLauncherImplTest {

	private Workspace	ws;
	private Project		project;
	private File		tmp;
	private File		launcherJar;

	@BeforeEach
	protected void setUp(TestInfo testInfo) throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + testInfo.getTestClass()
			.get()
			.getName() + "/"
			+ testInfo.getTestMethod()
				.get()
				.getName())
			.getAbsoluteFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);
		IO.copy(IO.getFile("testresources/ws"), tmp);
		ws = new Workspace(tmp);
		project = ws.getProject("p1");
		project.prepare();

		launcherJar = File.createTempFile("launcher", ".jar", tmp);
		try (Jar jar = new Jar("launcher")) {
			jar.putResource(ProjectLauncherImpl.PRE_JAR, new JarResource(new Jar("pre"), true));
			jar.write(launcherJar);
		}

	}

	@AfterEach
	protected void tearDown() throws Exception {
		IO.close(project);
		IO.close(ws);
	}

	@Test
	public void testParseSystemCapabilities() throws Exception {
		try (ProjectLauncherImpl launcher = new ProjectLauncherImpl(project, new Container(project, launcherJar))) {
			launcher.updateFromProject();
			launcher.prepare();

			String systemCaps = launcher.getSystemCapabilities();
			assertEquals(
				"osgi.native;osgi.native.osname:List<String>=\"Win7,Windows7,Windows 7\";osgi.native.osversion:Version=\"6.1\"",
				systemCaps);
		}
	}

	@Test
	public void testCwdIsProjectBase() throws Exception {
		try (ProjectLauncherImpl launcher = new ProjectLauncherImpl(project, new Container(project, launcherJar))) {
			launcher.updateFromProject();
			assertEquals(project.getBase(), launcher.getCwd());
		}
	}
}
