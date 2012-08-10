package test.deployer;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.security.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.lib.deployer.*;

public class FileRepoTest extends TestCase {
	
	private static FileRepo	testRepo;
	private static FileRepo	nonExistentRepo;

	private static String hashToString(byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	private static byte[] calculateHash(MessageDigest algorithm, File file) throws Exception {
		algorithm.reset();
		copy(file, algorithm);
		return algorithm.digest();
	}

	@Override
	protected void setUp() throws Exception {
		File testRepoDir = new File("src/test/repo");
		assertTrue(testRepoDir.isDirectory());
		testRepo = createRepo(testRepoDir);
		
		File nonExistentDir = new File("definitely/doesnt/exist");
		delete(nonExistentDir);
		assertFalse(nonExistentDir.exists());
		nonExistentRepo = createRepo(nonExistentDir);
	}
	
	private static FileRepo createRepo(File root) {
		FileRepo repo = new FileRepo();
		
		Map<String,String> props = new HashMap<String,String>();
		props.put("location", root.getAbsolutePath());
		repo.setProperties(props);
		
		return repo;
	}
	
	public static void testListBSNs() throws Exception {
		List<String> list = testRepo.list(null);
		assertNotNull(list);
		assertEquals(4, list.size());
		
		assertTrue(list.contains("ee.minimum"));
		assertTrue(list.contains("org.osgi.impl.service.cm"));
		assertTrue(list.contains("org.osgi.impl.service.io"));
		assertTrue(list.contains("osgi"));
	}

	public static void testListNonExistentRepo() throws Exception {
		// Listing should succeed and return non-null empty list
		List<String> list = nonExistentRepo.list(null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}
	
	public static void testBundleNotModifiedOnPut() throws Exception {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		File dstBundle = null;
		try {
			File srcBundle = new File("test/test.jar");
			byte[] srcSha = calculateHash(sha1, srcBundle);

			PutOptions options = new RepositoryPlugin.PutOptions();
			options.digest = srcSha;
			options.generateDigest = true;

			PutResult r = testRepo.put(new BufferedInputStream(new FileInputStream(srcBundle)), options);

			dstBundle = new File(r.artifact);

			assertEquals(hashToString(srcSha), hashToString(r.digest));
			assertTrue(MessageDigest.isEqual(srcSha, r.digest));
		}
		finally {
			if (dstBundle != null) {
				delete(dstBundle.getParentFile());
			}
		}
	}

	public static void testDeployToNonexistentRepoFails() throws Exception {
		try {
			nonExistentRepo.put(new BufferedInputStream(new FileInputStream("test/test.jar")),
					new RepositoryPlugin.PutOptions());
			fail("Should have thrown exception");
		}
		catch (Exception e) {
			assert(e instanceof IOException);
			String s = "Repository directory " + nonExistentRepo.getRoot() + " is not a directory";
			assertEquals(s, e.getMessage());
		}
	}
}
