package test.deployer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.deployer.FileRepo;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.map.MAP;

@SuppressWarnings("resource")
public class FileRepoTest {

	private FileRepo	testRepo;
	private FileRepo	nonExistentRepo;
	private FileRepo	indexedRepo;
	private File		tmp;

	private String hashToString(byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	private byte[] calculateHash(MessageDigest algorithm, File file) throws Exception {
		algorithm.reset();
		IO.copy(file, algorithm);
		return algorithm.digest();
	}

	@BeforeEach
	protected void setUp(TestInfo info) throws Exception {
		Method testMethod = info.getTestMethod()
			.get();
		tmp = new File("generated/tmp/test/" + getClass().getName() + "/" + testMethod.getName()).getAbsoluteFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);

		File testRepoDir = IO.getFile("test/test/repo");
		assertTrue(testRepoDir.isDirectory());
		testRepo = createRepo(testRepoDir);

		File nonExistentDir = IO.getFile(tmp, "invalidrepo");
		IO.mkdirs(nonExistentDir);
		nonExistentDir.setReadOnly();
		nonExistentRepo = createRepo(nonExistentDir);

		indexedRepo = createRepo(tmp, MAP.$("index", "true"));
	}

	private FileRepo createRepo(File root) {
		return createRepo(root, new HashMap<>());
	}

	private FileRepo createRepo(File root, Map<String, String> props) {
		FileRepo repo = new FileRepo();

		props.put("location", root.getAbsolutePath());
		repo.setProperties(props);

		return repo;
	}

	/**
	 * Test a repo with an index
	 */
	@Test
	public void testIndex() throws Exception {

		//
		// Check if the index property works
		// by verifying the diff between the
		// testRepo and the indexed Repo
		//

		assertNull(testRepo.getResources());
		assertNotNull(indexedRepo.getResources());

		//
		// Check that we can actually put a resource
		//

		PutResult put = indexedRepo.put(IO.getFile("jar/osgi.jar")
			.toURI()
			.toURL()
			.openStream(), null);
		assertNotNull(put);

		// Can we get it?

		ResourceDescriptor desc = indexedRepo.getDescriptor("osgi", new Version("4.0"));
		assertNotNull(desc);

		// Got the same file?

		assertTrue(Arrays.equals(put.digest, desc.id));

		//
		// Check if the description was copied
		//

		assertEquals("OSGi Service Platform Release 4 Interfaces and Classes for use in compiling bundles.",
			desc.description);

		//
		// We must be able to access by its sha1
		//

		ResourceDescriptor resource = indexedRepo.getResource(put.digest);
		assertTrue(Arrays.equals(resource.id, desc.id));

		//
		// Check if we now have a set of resources
		//
		SortedSet<ResourceDescriptor> resources = indexedRepo.getResources();
		assertEquals(1, resources.size());
		ResourceDescriptor rd = resources.iterator()
			.next();
		assertTrue(Arrays.equals(rd.id, put.digest));

		//
		// Check if the bsn brings us back
		//
		File file = indexedRepo.get(desc.bsn, desc.version, null);
		assertNotNull(file);
		assertTrue(Arrays.equals(put.digest, SHA1.digest(file)
			.digest()));
		byte[] digest = SHA256.digest(file)
			.digest();
		assertTrue(Arrays.equals(rd.sha256, digest));

		//
		// Delete and see if it is really gone
		//
		indexedRepo.delete(desc.bsn, desc.version);
		resources = indexedRepo.getResources();
		assertEquals(0, resources.size());

		file = indexedRepo.get(desc.bsn, desc.version, null);
		assertNull(file);

		resource = indexedRepo.getResource(put.digest);
		assertNull(resource);
	}

	@Test
	public void testListBSNs() throws Exception {
		List<String> list = testRepo.list(null);
		assertThat(list).containsOnly("ee.minimum", "org.osgi.impl.service.cm", "org.osgi.impl.service.io", "osgi");
	}

	@Test
	public void testListNonExistentRepo() throws Exception {
		// Listing should succeed and return non-null empty list
		List<String> list = nonExistentRepo.list(null);
		assertThat(list).isEmpty();
	}

	@Test
	public void testBundleNotModifiedOnPut() throws Exception {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		File dstBundle = null;
		try {
			File srcBundle = IO.getFile("testresources/test.jar");
			byte[] srcSha = calculateHash(sha1, srcBundle);

			PutOptions options = new RepositoryPlugin.PutOptions();
			options.digest = srcSha;

			PutResult r = testRepo.put(new BufferedInputStream(new FileInputStream(srcBundle)), options);

			dstBundle = new File(r.artifact);

			assertEquals(hashToString(srcSha), hashToString(r.digest));
			assertTrue(MessageDigest.isEqual(srcSha, r.digest));
		} finally {
			if (dstBundle != null) {
				IO.delete(dstBundle.getParentFile());
			}
		}
	}

	@Test
	public void testDownloadListenerCallback() throws Exception {
		FileRepo repo = new FileRepo("tmp", tmp, true);
		File srcBundle = IO.getFile("testresources/test.jar");

		PutResult r = repo.put(IO.stream(IO.getFile("testresources/test.jar")), null);

		assertNotNull(r);
		assertNotNull(r.artifact);
		File f = new File(r.artifact); // file repo, so should match
		SHA1 sha1 = SHA1.digest(srcBundle);
		sha1.equals(SHA1.digest(f));

		DownloadListener mock = Mockito.mock(DownloadListener.class);

		f = repo.get("test", new Version("0"), null, mock);
		Mockito.verify(mock)
			.success(f);
		Mockito.verifyNoMoreInteractions(mock);
		Mockito.reset(mock);

		f = repo.get("XXXXXXXXXXXXXXXXX", new Version("0"), null, mock);
		assertNull(f);
		Mockito.verifyZeroInteractions(mock);
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	public void testDeployToNonexistentRepoFails() throws Exception {
		assertThatThrownBy(() -> nonExistentRepo.put(
			new BufferedInputStream(new FileInputStream("testresources/test.jar")), new RepositoryPlugin.PutOptions()));
	}

	@Test
	public void testCommands() throws Exception {
		FileRepo repo = new FileRepo();
		File root = tmp;
		IO.delete(root);
		SoftAssertions softly = new SoftAssertions();

		Map<String, String> props = new HashMap<>();
		props.put(FileRepo.LOCATION, root.getAbsolutePath());
		props.put(FileRepo.CMD_INIT, "echo init $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_OPEN, "echo open $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_BEFORE_GET, "echo beforeGet $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_BEFORE_PUT, "echo beforePut $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_AFTER_PUT, "echo afterPut $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_ABORT_PUT, "echo abortPut $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_REFRESH, "echo refresh  $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_CLOSE, "echo close  $0 $1 $2 $3 >>report");
		props.put(FileRepo.CMD_PATH, "/xxx,$@,/yyy");
		props.put(FileRepo.TRACE, true + "");
		repo.setProperties(props);

		repo.refresh();
		{
			try (InputStream in = IO.stream(IO.getFile("jar/osgi.jar"))) {
				repo.put(in, null);
			}
		}
		{
			try (InputStream in = IO.stream("not a valid zip")) {
				softly.assertThatThrownBy(() -> repo.put(in, null));
			}
		}
		repo.close();
		String s = IO.collect(new File(root, "report"));
		System.out.println(s);
		s = s.replace('\\', '/');
		s = s.replaceAll(Pattern.quote(root.getAbsolutePath()
			.replace('\\', '/')), "@");

		String parts[] = s.split("\r?\n");

		softly.assertThat(parts)
			.hasSize(8);
		int size = parts.length;
		if (size > 0) {
			softly.assertThat(parts[0])
				.matches("init\\s+@\\s*");
		}
		if (size > 1) {
			softly.assertThat(parts[1])
				.matches("open\\s+@\\s*");
		}
		if (size > 2) {
			softly.assertThat(parts[2])
				.matches("refresh\\s+@\\s*");
		}
		if (size > 3) {
			softly.assertThat(parts[3])
				.matches("beforePut\\s+@\\s+@/.*");
		}
		if (size > 4) {
			softly.assertThat(parts[4])
				.matches(
					"afterPut\\s+@\\s+@/osgi/osgi-4\\.0\\.0\\.jar\\s+D37A1C9D5A9D3774F057B5452B7E47B6D1BB12D0\\s*");
		}
		if (size > 5) {
			softly.assertThat(parts[5])
				.matches("beforePut\\s+@\\s+@/.*");
		}
		if (size > 6) {
			softly.assertThat(parts[6])
				.matches("abortPut\\s+@\\s+@/.*");
		}
		if (size > 7) {
			softly.assertThat(parts[7])
				.matches("close\\s+@\\s*");
		}
		softly.assertAll();
	}
}
