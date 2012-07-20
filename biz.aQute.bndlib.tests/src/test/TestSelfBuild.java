package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;

public class TestSelfBuild extends TestCase {

	public void testSelfBuild() throws Throwable {
		Project project = Workspace.getWorkspace(new File("").getAbsoluteFile().getParentFile()).getProject(
				"biz.aQute.bndlib");
		project.setPedantic(true);
		project.action("build");

		File files[] = project.build();
		assertTrue(project.check("Imports that lack version ranges"));
		assertNotNull(files);
		assertEquals(1, files.length);
	}
}
