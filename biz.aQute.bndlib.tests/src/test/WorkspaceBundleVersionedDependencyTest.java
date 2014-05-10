package test;

import java.io.*;

import aQute.bnd.build.*;
import junit.framework.*;

/**
 * Tests if it is possible to depend on workspace bundles (not released) using a specific version.
 */
public class WorkspaceBundleVersionedDependencyTest extends TestCase{
	
	public static void testWorkspaceVersionedDependency() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws-versioneddependencies"));
		
		ws.getProject("mydependency").build();
		Project project = ws.getProject("myconsumer");
		project.clean();
		project.build();
		assertTrue(project.isOk());
	}
}
