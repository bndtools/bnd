package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;

public class WorkspaceExtensionsTest {
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;

	@Before
	public void setUp() throws IOException {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
	}

	@Test
	public void testSimpleExtension() throws Exception {
		IO.copy(IO.getFile("testresources/ws-workspaceextensions-simple"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			String testextension = ws.getProperty("workspaceextension");
			assertThat(testextension).isNotEmpty()
				.isEqualTo("test");
		}
	}

	@Test
	public void testExtensionExtension() throws Exception {
		IO.copy(IO.getFile("testresources/ws-workspaceextensions-extension"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			String rootextension = ws.getProperty("rootvar");
			assertThat(rootextension).isNotEmpty()
				.isEqualTo("test");
			String testextension = ws.getProperty("workspaceextension");
			assertThat(testextension).isNotEmpty()
				.isEqualTo("test");
		}
	}

	@Test
	public void testSimpleProjectExtension() throws Exception {
		IO.copy(IO.getFile("testresources/ws-workspaceextensions-project-simple"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			String testextension = ws.getProperty("workspaceextension");
			assertThat(testextension).isNotEqualTo("test");
			Project p1 = ws.getProject("p1");
			String testextensionProject = p1.getProperty("workspaceextension");
			assertThat(testextensionProject).isNotEmpty()
				.isEqualTo("test");

		}
	}
}
