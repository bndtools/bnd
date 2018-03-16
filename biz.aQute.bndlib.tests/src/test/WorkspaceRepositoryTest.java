package test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class WorkspaceRepositoryTest extends TestCase {
	static Workspace			workspace;
	static WorkspaceRepository	repo;

	private static void reallyClean(Workspace ws) throws Exception {
		String wsName = ws.getBase()
			.getName();
		for (Project project : ws.getAllProjects()) {
			if (("p1".equals(project.getName()) && "ws-repo-test".equals(wsName))
				|| ("p2".equals(project.getName()) && "ws-repo-test".equals(wsName))
				|| ("p3".equals(project.getName()) && "ws-repo-test".equals(wsName))) {
				File output = project.getSrcOutput()
					.getAbsoluteFile();
				if (output.isDirectory() && output.getParentFile() != null) {
					IO.delete(output);
				}
			} else {
				project.clean();

				File target = project.getTargetDir();
				if (target.isDirectory() && target.getParentFile() != null) {
					IO.delete(target);
				}
				File output = project.getSrcOutput()
					.getAbsoluteFile();
				if (output.isDirectory() && output.getParentFile() != null) {
					IO.delete(output);
				}
			}
		}
		IO.delete(ws.getFile("cnf/cache"));
	}

	public static void testIMustBeUpdated() {

	}

	@Override
	public void setUp() throws Exception {
		workspace = new Workspace(IO.getFile("testresources/ws-repo-test"));
		repo = new WorkspaceRepository(workspace);
	}

	@Override
	public void tearDown() throws Exception {
		IO.deleteWithException(new File("tmp-ws"));
		reallyClean(workspace);
	}

	public void testListNoFilter() throws Exception {
		List<String> files = repo.list(null);
		assertNotNull(files);
		assertTrue(files.size() > 0);
	}

	public void testListBsn() throws Exception {
		List<String> files = repo.list("p1");
		assertTrue(workspace.check());
		assertNotNull(files);
		assertTrue(files.size() > 0);
	}

	public void testListBsnForSubProject() throws Exception {
		List<String> files = repo.list("p4-sub.a");
		assertTrue(workspace.check());
		assertNotNull(files);
		assertTrue(files.size() > 0);
	}

	public void testListNotExisting() throws Exception {
		List<String> files = repo.list("somenotexistingproject");
		assertTrue(workspace.check());
		assertNotNull(files);
		assertEquals(0, files.size());
	}

	public void testVersionsSingle() throws Exception {
		SortedSet<Version> versions = repo.versions("p2");
		assertTrue(workspace.check());
		assertNotNull(versions);
		assertEquals(1, versions.size());
	}

	public void testVersionsNotExistingBsn() throws Exception {
		SortedSet<Version> versions = repo.versions("somenotexistingproject");
		assertTrue(workspace.check());
		assertNotNull(versions);
		assertEquals(0, versions.size());
	}

	public void testVersionsTranslateFromMavenStyle() throws Exception {
		SortedSet<Version> versions = repo.versions("p5");
		assertTrue(workspace.check());
		assertNotNull(versions);
		assertEquals(1, versions.size());
		assertEquals("1.0.0.FOOBAR", versions.iterator()
			.next()
			.toString());
	}

	public void testGetName() {
		assertEquals("Workspace ws-repo-test", repo.getName());
	}

	public void testGetExact() throws Exception {
		File file = repo.get("p2", new Version("1.2.3"), new HashMap<>());
		assertTrue(workspace.check());
		assertNotNull(file);
	}
}
