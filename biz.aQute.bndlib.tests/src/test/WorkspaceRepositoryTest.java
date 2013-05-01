package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;

public class WorkspaceRepositoryTest extends TestCase {
	static Workspace			workspace;
	static WorkspaceRepository	repo;

	public static void testIMustBeUpdated() {
		
	}
	public void setUp() throws Exception {
		workspace = Workspace.getWorkspace(new File("test/ws-repo-test"));
		repo = new WorkspaceRepository(workspace);
	}

	public void tearDown() throws Exception {
		IO.deleteWithException(new File("tmp-ws"));
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
		assertEquals("1.0.0.FOOBAR", versions.iterator().next().toString());
	}

	public void testGetName() {
		assertEquals("Workspace ws-repo-test", repo.getName());
	}

	public void testGetExact() throws Exception {
		File file = repo.get("p2", new Version("1.2.3"), new HashMap<String,String>());
		assertTrue(workspace.check());
		assertNotNull(file);
	}
}
