package test.deployer;

import java.io.*;
import java.security.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.osgi.*;
import aQute.lib.deployer.*;
import aQute.lib.io.*;

public class FileRepoTest extends TestCase {
	
	private FileRepo	testRepo;
	private FileRepo	nonExistentRepo;

	private static String hashToString(byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	private static byte[] calculateHash(MessageDigest algorithm, File file) throws Exception {
		algorithm.reset();
		IO.copy(file, algorithm);
		return algorithm.digest();
	}

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
	
	public void testBundleNotModifiedOnPut() throws Exception {
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		Jar srcJar = null;
		File dstBundle = null;
		try {
			File srcBundle = new File("test/test.jar");
			byte[] srcSha = calculateHash(sha1, srcBundle);

			srcJar = new Jar(srcBundle);

			dstBundle = testRepo.put(srcJar);
			byte[] dstSha = calculateHash(sha1, dstBundle);

			assertEquals(hashToString(srcSha), hashToString(dstSha));
			assertTrue(MessageDigest.isEqual(srcSha, dstSha));
		}
		finally {
			if (srcJar != null)
				srcJar.close();
			if (dstBundle != null) {
				IO.delete(dstBundle.getParentFile());
			}
		}
	}

	//	public void testDeployToNonexistentRepoFails() throws Exception {
	//		Jar bundleJar = new Jar(new File("test/test.jar"));
	//		try {
	//			nonExistentRepo.put(bundleJar);
	//			fail("Should have thrown exception");
	//		} catch (Exception e) {
	//			// Expected
	//		} finally {
	//			bundleJar.close();
	//		}
	//	}
}
