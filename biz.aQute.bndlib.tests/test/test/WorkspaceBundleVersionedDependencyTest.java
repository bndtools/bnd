package test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

/**
 * Tests if it is possible to depend on workspace bundles (not released) using a
 * specific version.
 */
public class WorkspaceBundleVersionedDependencyTest {
	@InjectTemporaryDirectory
	File testDir;

	@Test
	public void testWorkspaceVersionedDependency() throws Exception {
		try (Workspace ws = new Workspace(IO.copy(new File("testresources/ws-versioneddependencies"),
			new File(testDir, "ws-versioneddependencies")))) {
			Project project = ws.getProject("myconsumer");
			project.clean();
			project.build();
			assertTrue(project.check());
		}
	}

	@Test
	public void testWorkspaceVersionedDependencyWithSubbundle() throws Exception {
		try (Workspace ws = new Workspace(IO.copy(new File("testresources/ws-versioneddependencies-withsubbundle"),
			new File(testDir, "ws-versioneddependencies-withsubbundle")))) {
			ws.getProject("mydependency")
				.build();
			Project project = ws.getProject("myconsumer");
			project.clean();
			project.build();
			assertTrue(project.check());
		}

	}
}
