package aQute.bnd.eclipse;

import java.io.File;
import java.util.HashSet;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.util.UpdatePaths;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class PdeImportTest extends TestCase {

	File tmp = IO.getFile("generated/tmp");

	public void testSimple() throws Exception {
		IO.delete(tmp);
		File tgt = new File(tmp, "ws1");
		IO.copy(IO.getFile("testresources/aQute.bnd.eclipse/ws1"), tgt);
		Workspace ws = Workspace.getWorkspace(tgt);

		File p1File = IO.getFile("testresources/aQute.bnd.eclipse/pdeproj/p1");
		File p2File = IO.getFile("testresources/aQute.bnd.eclipse/pdeproj/p2");
		Project p1;
		Project p2;

		try (LibPde l = new LibPde(ws, p1File)) {
			p1 = l.write();

			assertTrue(l.check());
		}
		try (LibPde l = new LibPde(ws, p2File)) {
			p2 = l.write();

			assertTrue(l.check());
		}

		try (UpdatePaths updater = new UpdatePaths(ws)) {
			updater.updateProject(p1, new HashSet<>());
		}
		try (UpdatePaths updater = new UpdatePaths(ws)) {
			updater.updateProject(p2, new HashSet<>());
		}

	}
}
