package test;

import java.io.*;

import aQute.bnd.build.*;
import aQute.lib.io.*;
import junit.framework.*;

/**
 * Tests if it is possible to depend on workspace bundles (not released) using a specific version.
 */
public class WorkspaceBundleVersionedDependencyTest extends TestCase{
	
	public static void testWorkspaceVersionedDependency() throws Exception {
		IO.copy(new File("testresources/ws-versioneddependencies"), new File("generated/ws-versioneddependencies"));
		Workspace ws = Workspace.getWorkspace(new File("generated/ws-versioneddependencies"));
		
		Project project = ws.getProject("myconsumer");
		project.clean();
		project.build();
		assertTrue(project.check());
	}
	
	public static void testWorkspaceVersionedDependencyWithSubbundle() throws Exception {
		IO.copy(new File("testresources/ws-versioneddependencies-withsubbundle"), new File("generated/ws-versioneddependencies-withsubbundle"));
		Workspace ws = Workspace.getWorkspace(new File("generated/ws-versioneddependencies-withsubbundle"));
		
		ws.getProject("mydependency").build();
		Project project = ws.getProject("myconsumer");
		project.clean();
		project.build();
		assertTrue(project.check());
	}
}
