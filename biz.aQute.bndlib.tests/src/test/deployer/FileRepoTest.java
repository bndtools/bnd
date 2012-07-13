package test.deployer;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.osgi.*;
import aQute.lib.deployer.*;

public class FileRepoTest extends TestCase {
	
	private FileRepo	testRepo;
	private FileRepo	nonExistentRepo;

	@Override
	protected void setUp() throws Exception {
		File testRepoDir = new File("src/test/repo");
		assertTrue(testRepoDir.isDirectory());
		testRepo = createRepo(testRepoDir);
		
		File nonExistentDir = new File("definitely/doesnt/exist");
		assertFalse(nonExistentDir.exists());
		nonExistentRepo = createRepo(nonExistentDir);
	}
	
	private FileRepo createRepo(File root) {
		FileRepo repo = new FileRepo();
		
		Map<String,String> props = new HashMap<String,String>();
		props.put("location", root.getAbsolutePath());
		repo.setProperties(props);
		
		return repo;
	}
	
	public void testListBSNs() throws Exception {
		List<String> list = testRepo.list(null);
		assertNotNull(list);
		assertEquals(4, list.size());
		
		assertTrue(list.contains("ee.minimum"));
		assertTrue(list.contains("org.osgi.impl.service.cm"));
		assertTrue(list.contains("org.osgi.impl.service.io"));
		assertTrue(list.contains("osgi"));
	}

	public void testListNonExistentRepo() throws Exception {
		// Listing should succeed and return non-null empty list
		List<String> list = nonExistentRepo.list(null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}
	
	public void testDeployToNonexistentRepoFails() throws Exception {
		Jar bundleJar = new Jar(new File("test/test.jar"));
		try {
			nonExistentRepo.put(bundleJar);
			fail("Should have thrown exception");
		} catch (Exception e) {
			// Expected
		} finally {
			bundleJar.close();
		}
	}
}
