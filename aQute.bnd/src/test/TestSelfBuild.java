package test;

import java.io.File;

import junit.framework.TestCase;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class TestSelfBuild extends TestCase {

	public void testSelfBuild() throws Throwable {
		Project project = Workspace.getWorkspace( new File("").getAbsoluteFile().getParentFile() ).getProject("aQute.bnd");
		project.setTrace(true);
		project.setPedantic(true);
		project.action("build");
		
		File files[] = project.build();
		assertEquals(0, project.getErrors().size(), 0);
		assertEquals(0, project.getWarnings().size(), 0);
		assertNotNull(files);
		assertEquals(1,files.length);
	}
}
