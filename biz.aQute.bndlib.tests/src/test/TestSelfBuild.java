package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.lib.osgi.*;

public class TestSelfBuild extends TestCase {

	public void testSelfBuild() throws Throwable {
		Project project = Workspace.getWorkspace( new File("").getAbsoluteFile().getParentFile() ).getProject("biz.aQute.bndlib");
		project.setPedantic(true);
		project.action("build");
		
		File files[] = project.build();
		System.out.println(Processor.join(project.getErrors(),"\n"));
		System.out.println(Processor.join(project.getWarnings(),"\n"));
		assertEquals(0, project.getErrors().size());
		assertEquals(0, project.getWarnings().size());
		assertNotNull(files);
		assertEquals(1,files.length);
	}
}
