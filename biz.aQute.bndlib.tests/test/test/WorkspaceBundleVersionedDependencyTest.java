package test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import junit.framework.TestCase;

/**
 * Tests if it is possible to depend on workspace bundles (not released) using a
 * specific version.
 */
public class WorkspaceBundleVersionedDependencyTest extends TestCase {

	public static void testWorkspaceVersionedDependency() throws Exception {
		IO.copy(IO.getFile("testresources/ws-versioneddependencies"), IO.getFile("generated/ws-versioneddependencies"));
		Workspace ws = Workspace.getWorkspace(IO.getFile("generated/ws-versioneddependencies"));

		Project project = ws.getProject("myconsumer");
		project.clean();
		project.build();
		assertTrue(project.check());
	}

	public static void testWorkspaceVersionedDependencyWithSubbundle() throws Exception {
		IO.copy(IO.getFile("testresources/ws-versioneddependencies-withsubbundle"),
			IO.getFile("generated/ws-versioneddependencies-withsubbundle"));
		Workspace ws = Workspace.getWorkspace(IO.getFile("generated/ws-versioneddependencies-withsubbundle"));

		ws.getProject("mydependency")
			.build();
		Project project = ws.getProject("myconsumer");
		project.clean();
		project.build();
		assertTrue(project.check());
	}
}
