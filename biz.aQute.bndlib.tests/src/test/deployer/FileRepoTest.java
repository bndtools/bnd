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

		File nonExistentDir = new File("invalidrepo");
		nonExistentDir.mkdir();
		nonExistentDir.setReadOnly();
		nonExistentRepo = createRepo(nonExistentDir);
	}
	
	@Override
	protected void tearDown() throws Exception {
		File nonExistentDir = new File("invalidrepo");		
		delete(nonExistentDir);		
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
			// OK, you cannot check for exception messages or exception type
		}
	}

	public void testCommands() throws Exception {
		FileRepo repo = new FileRepo();
		File root = new File("tmp");
		delete(root);
		try {
			Map<String,String> props = new HashMap<String,String>();
			props.put(FileRepo.LOCATION, root.getAbsolutePath());
			props.put(FileRepo.CMD_INIT, "echo init>>report");
			props.put(FileRepo.CMD_OPEN, "echo open >>report");
			props.put(FileRepo.CMD_BEFORE_GET, "echo beforeGet ${@} >>report");
			props.put(FileRepo.CMD_BEFORE_PUT, "echo beforePut >>report");
			props.put(FileRepo.CMD_AFTER_PUT, "echo afterPut ${@} >>report");
			props.put(FileRepo.CMD_ABORT_PUT, "echo abortPut >>report");
			props.put(FileRepo.CMD_REFRESH, "echo refresh >>report");
			props.put(FileRepo.CMD_CLOSE, "echo close >>report");
			props.put(FileRepo.CMD_PATH, "/xxx,${@},/yyy");
			props.put(FileRepo.TRACE, true+"");
			repo.setProperties(props);

			repo.refresh();
			{
				InputStream in = stream(getFile("jar/osgi.jar"));
				try {
					repo.put(in, null);

				}
				finally {
					in.close();
				}
			}
			{
				InputStream in = stream("not a valid zip");
				try {
					repo.put(in, null);
					fail("expected failure");
				} catch( Exception e) {
					// ignore
				}
				finally {
					in.close();
				}
			}
			repo.close();
			String s = collect(new File(root, "report"));
			s = s.replaceAll("\r?\n", "@");
			System.out.println(s);
			assertTrue(s.matches("init@open@refresh@beforePut@afterPut .*tmp/osgi/osgi-4.0.0.jar@beforePut@abortPut@close@"));
		}
		finally {
			delete(root);
		}

	}
}
