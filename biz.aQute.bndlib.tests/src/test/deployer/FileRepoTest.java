package test.deployer;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.security.*;
import java.util.*;

import junit.framework.*;

import org.mockito.*;

import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.version.*;
import aQute.lib.deployer.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;

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

	public static void testDownloadListenerCallback() throws Exception {
		File tmp = new File("tmp");
		try {
			FileRepo repo = new FileRepo("tmp", tmp, true);
			File srcBundle = new File("test/test.jar");

			PutResult r = repo.put(IO.stream(new File("test/test.jar")), null);

			assertNotNull(r);
			assertNotNull(r.artifact);
			File f = new File(r.artifact); // file repo, so should match
			SHA1 sha1 = SHA1.digest(srcBundle);
			sha1.equals(SHA1.digest(f));

			DownloadListener mock = Mockito.mock(DownloadListener.class);

			f = repo.get("test", new Version("0"), null, mock);
			Mockito.verify(mock).success(f);
			Mockito.verifyNoMoreInteractions(mock);
			Mockito.reset(mock);

			f = repo.get("XXXXXXXXXXXXXXXXX", new Version("0"), null, mock);
			assertNull(f);
			Mockito.verifyZeroInteractions(mock);
		}
		finally {
			IO.delete(tmp);
		}
	}

	public static void testDeployToNonexistentRepoFails() throws Exception {

		if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0 ) {
			// File#setReadonly() is broken on windows
			return;
		}
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
			props.put(FileRepo.CMD_INIT, "echo init $0 $1 $2 $3>>report");
			props.put(FileRepo.CMD_OPEN, "echo open $0 $1 $2 $3 >>report");
			props.put(FileRepo.CMD_BEFORE_GET, "echo beforeGet $0 $1 $2 $3 >>report");
			props.put(FileRepo.CMD_BEFORE_PUT, "echo beforePut $0 $1 $2 $3>>report");
			props.put(FileRepo.CMD_AFTER_PUT, "echo afterPut $0 $1 $2 $3>>report");
			props.put(FileRepo.CMD_ABORT_PUT, "echo abortPut $0 $1 $2 $3>>report");
			props.put(FileRepo.CMD_REFRESH, "echo refresh  $0 $1 $2 $3>>report");
			props.put(FileRepo.CMD_CLOSE, "echo close  $0 $1 $2 $3>>report");
			props.put(FileRepo.CMD_PATH, "/xxx,$@,/yyy");
			props.put(FileRepo.TRACE, true + "");
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
				}
				catch (Exception e) {
					// ignore
				}
				finally {
					in.close();
				}
			}
			repo.close();
			String s = collect(new File(root, "report"));
			s = s.replaceAll(root.getAbsolutePath(), "@");
			s = s.replaceAll(File.separator, "/");
			System.out.println(s);

			String parts[] = s.split("\r?\n");
			assertEquals(8, parts.length);
			assertEquals(parts[0], "init @");
			assertEquals(parts[1], "open @");
			assertEquals(parts[2], "refresh @");
			assertTrue(parts[3].matches("beforePut @ @/.*"));
			assertEquals(parts[4],
					"afterPut @ @/osgi/osgi-4.0.0.jar D37A1C9D5A9D3774F057B5452B7E47B6D1BB12D0");
			assertTrue(parts[5].matches("beforePut @ @/.*"));
			assertTrue(parts[6].matches("abortPut @ @/.*"));
			assertEquals(parts[7], "close @");
		}
		finally {
			delete(root);
		}

	}
}
