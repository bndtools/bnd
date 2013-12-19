package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;

public class TestSelfBuild extends TestCase {

	public static void testSelfBuild() throws Throwable {
		Project project = Workspace.getWorkspace(new File("").getAbsoluteFile().getParentFile()).getProject(
				"biz.aQute.bndlib");
		project.setPedantic(true);

		File files[] = project.build();
		assertTrue( project.check("Imports that lack version ranges: \\[javax"));
		assertNotNull(files);
		assertEquals(1, files.length);
	}
}
