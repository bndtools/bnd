package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.io.*;
import aQute.libg.version.*;

public class WorkspaceRepositoryTest extends TestCase {
	Workspace			workspace;
	WorkspaceRepository	repo;

	public void testIMustBeUpdated() {
		
	}
//	public void setUp() throws Exception {
//		File tmp = new File("tmp-ws");
//		if (tmp.exists())
//			IO.deleteWithException(tmp);
//		tmp.mkdir();
//		assertTrue(tmp.isDirectory());
//		IO.copy(new File(new File("test"), "ws"), tmp);
//
//		workspace = Workspace.getWorkspace(tmp);
//		repo = new WorkspaceRepository(workspace);
//	}
//
//	public void tearDown() throws Exception {
//		IO.deleteWithException(new File("tmp-ws"));
//	}
//
//	public void testListNoFilter() throws Exception {
//		List<String> files = repo.list(null);
//		assertTrue(workspace.check("embedded-repo"));
//		assertNotNull(files);
//		assertTrue(files.size() > 0);
//	}
//
//	public void testListBsn() throws Exception {
//		List<String> files = repo.list("p1");
//		assertTrue(workspace.check());
//		assertNotNull(files);
//		assertTrue(files.size() > 0);
//	}
//
//	public void testListBsnForSubProject() throws Exception {
//		List<String> files = repo.list("p4-sub.a");
//		assertTrue(workspace.check());
//		assertNotNull(files);
//		assertTrue(files.size() > 0);
//	}
//
//	public void testListNotExisting() throws Exception {
//		List<String> files = repo.list("somenotexistingproject");
//		assertTrue(workspace.check());
//		assertNotNull(files);
//		assertEquals(0, files.size());
//	}
//
//	public void testVersionsSingle() throws Exception {
//		List<Version> versions = repo.versions("p2");
//		assertTrue(workspace.check());
//		assertNotNull(versions);
//		assertEquals(1, versions.size());
//	}
//
//	public void testVersionsNotExistingBsn() throws Exception {
//		List<Version> versions = repo.versions("somenotexistingproject");
//		assertTrue(workspace.check());
//		assertNotNull(versions);
//		assertEquals(0, versions.size());
//	}
//
//	public void testGetName() {
//		assertEquals("Workspace", repo.getName());
//	}
//
//	public void testGetLatest() throws Exception {
//		File[] files = repo.get("p2", "latest");
//		assertTrue(workspace.check());
//		assertEquals(1, files.length);
//	}
//
//	public void testGetSingleHighest() throws Exception {
//		File file = repo.get("p2", "[1,2)", Strategy.HIGHEST, null);
//		assertTrue(workspace.check());
//		assertNotNull(file);
//	}
//
//	public void testGetExact() throws Exception {
//		File[] files = repo.get("p2", "1.2.3");
//		assertTrue(workspace.check());
//		assertEquals(1, files.length);
//	}
//
//	public void testGetRange() throws Exception {
//		File[] files = repo.get("p2", "[1,2)");
//		assertTrue(workspace.check());
//		assertEquals(1, files.length);
//	}
//
//	public void testGetRangeNotFound() throws Exception {
//		File[] files = repo.get("p2", "[2,3)");
//		assertTrue(workspace.check());
//		assertEquals(0, files.length);
//	}
}
