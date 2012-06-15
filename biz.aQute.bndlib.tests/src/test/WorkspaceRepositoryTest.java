package test;

import java.io.File;
import java.util.List;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.libg.version.Version;
import junit.framework.TestCase;

public class WorkspaceRepositoryTest extends TestCase {
	private WorkspaceRepository repo;
	
	public void setUp() throws Exception {
		Workspace workspace = Workspace.getWorkspace("test/ws");
		repo = new WorkspaceRepository(workspace);
	}
	
	public void testListNoFilter() throws Exception {
		List<String> files = repo.list(null);
		assertNotNull(files);
		assertTrue(files.size() > 0);
	}
	
	public void testListBsn() throws Exception {
		List<String> files = repo.list("p1");
		assertNotNull(files);
		assertTrue(files.size() > 0);
	}
	
	public void testListBsnForSubProject() throws Exception {
		List<String> files = repo.list("p4-sub.a");
		assertNotNull(files);
		assertTrue(files.size() > 0);
	}
	
	public void testListNotExisting() throws Exception {
		List<String> files = repo.list("somenotexistingproject");
		assertNotNull(files);
		assertEquals(0, files.size());
	}
	
	public void testVersionsSingle() throws Exception {
		List<Version> versions = repo.versions("p2");
		assertNotNull(versions);
		assertEquals(1, versions.size());
	}
		
	public void testVersionsNotExistingBsn() throws Exception {
		List<Version> versions = repo.versions("somenotexistingproject");
		assertNotNull(versions);
		assertEquals(0, versions.size());
	}

	public void testGetName() {
		assertEquals("Workspace", repo.getName());
	}
	
	public void testGetLatest() throws Exception{
		File[] files = repo.get("p2", "latest");
		assertEquals(1, files.length);
	}
	
	public void testGetSingleHighest() throws Exception{
		File file = repo.get("p2","[1,2)", Strategy.HIGHEST, null);
		assertNotNull(file);
	}
	
	public void testGetExact() throws Exception{
		File[] files = repo.get("p2", "1.2.3");
		assertEquals(1, files.length);
	}
	
	public void testGetRange() throws Exception{
		File[] files = repo.get("p2", "[1,2)");
		assertEquals(1, files.length);
	}
	
	public void testGetRangeNotFound() throws Exception{
		File[] files = repo.get("p2", "[2,3)");
		assertEquals(0, files.length);
	}
}
