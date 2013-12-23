package aQute.bnd.mavenplugin;

import java.io.*;
import java.util.*;

import org.apache.maven.plugin.*;
import org.apache.maven.project.*;
import org.junit.*;
import org.mockito.*;

import aQute.bnd.build.*;

public class BndTestRunnerTest {
	@Test
	public void testExecuteBndTestsSuccess() throws Exception {
		BndTestRunner btr = new BndTestRunner();
		btr.bndWorkspace = Mockito.mock(BndWorkspace.class);

		Project mockProject = Mockito.mock(Project.class);
		Mockito.when(mockProject.getErrors()).thenReturn(Collections.<String>emptyList());

		Workspace mockWS = Mockito.mock(Workspace.class);
		Mockito.when(mockWS.getProject("foo")).thenReturn(mockProject);

		Mockito.when(btr.bndWorkspace.getWorkspace(null)).thenReturn(mockWS);

		btr.mavenProject = Mockito.mock(MavenProject.class);
		Mockito.when(btr.mavenProject.getArtifactId()).thenReturn("foo");

		btr.execute();
		Mockito.verify(mockProject).test();
	}

	@Test
	public void testExecuteBndTestsFailure() throws Exception {
		File targetDir = new File(System.getProperty("java.io.tmpdir"));

		BndTestRunner btr = new BndTestRunner();
		btr.bndWorkspace = Mockito.mock(BndWorkspace.class);

		Project mockProject = Mockito.mock(Project.class);
		Mockito.when(mockProject.getErrors()).thenReturn(Collections.singletonList("failure"));
		Mockito.when(mockProject.getTargetDir()).thenReturn(targetDir);

		Workspace mockWS = Mockito.mock(Workspace.class);
		Mockito.when(mockWS.getProject("foo")).thenReturn(mockProject);

		Mockito.when(btr.bndWorkspace.getWorkspace(null)).thenReturn(mockWS);

		btr.mavenProject = Mockito.mock(MavenProject.class);
		Mockito.when(btr.mavenProject.getArtifactId()).thenReturn("foo");

		try {
			btr.execute();
			Assert.fail("Should have failed the execute() call with a MojoFailureException");
		} catch (MojoFailureException mfe) {
			// good!
			Assert.assertTrue("The error message should contain the location of the test reports",
					mfe.getMessage().contains(targetDir.getAbsolutePath() + File.separator + "test-reports"));
		}

		Mockito.verify(mockProject).test();
	}
}
