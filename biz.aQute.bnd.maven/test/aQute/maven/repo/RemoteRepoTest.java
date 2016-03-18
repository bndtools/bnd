package aQute.maven.repo;

import java.io.File;
import java.util.Arrays;

import aQute.bnd.http.HttpClient;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.maven.repo.provider.RemoteRepo;
import biz.aQute.http.testservers.HttpTestServer.Config;
import junit.framework.TestCase;

public class RemoteRepoTest extends TestCase {

	File		local	= IO.getFile("generated/local");
	File		remote	= IO.getFile("generated/remote");
	FakeNexus	fnx;
	RemoteRepo	repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
		IO.delete(remote);
		IO.delete(local);
		remote.mkdirs();
		local.mkdirs();
		repo = new RemoteRepo(new HttpClient(), fnx.getBaseURI() + "/repo/");
	}

	@Override
	protected void tearDown() throws Exception {
		fnx.close();
		super.tearDown();
	}

	public void testBasic() throws Exception {
		File localFoobar = IO.getFile(this.local, "some.jar");
		File localFoobarSha1 = IO.getFile(this.local, "some.jar.sha1");
		File localFoobarMD5 = IO.getFile(this.local, "some.jar.md5");
		File remoteFoobar = IO.getFile(this.remote, "foo/bar");
		File remoteFoobarSha1 = IO.getFile(this.remote, "foo/bar.sha1");
		File remoteFoobarMD5 = IO.getFile(this.remote, "foo/bar.md5");
		remoteFoobar.getParentFile().mkdirs();

		// Test does not exist
		assertFalse(remoteFoobar.exists());
		assertFalse(repo.fetch("foo/bar", localFoobar));
		assertFalse(localFoobar.exists());

		//
		// Create remote
		//

		IO.store("bla", remoteFoobar);
		assertTrue(remoteFoobar.isFile());
		assertEquals(3L, remoteFoobar.length());

		//
		// Fetch it, must exist now
		//

		assertTrue(repo.fetch("foo/bar", localFoobar));
		assertTrue(localFoobar.isFile());
		assertEquals(3L, localFoobar.length());

		//
		// Overwrite it
		//

		assertFalse(remoteFoobarSha1.isFile());
		assertFalse(remoteFoobarMD5.isFile());

		IO.store("overwrite", localFoobar);
		repo.store(localFoobar, "foo/bar");
		assertEquals(9L, remoteFoobar.length());
		assertTrue(remoteFoobarSha1.isFile());
		assertTrue(remoteFoobarMD5.isFile());

		byte[] sha1Expected = SHA1.digest("overwrite".getBytes()).digest();
		byte[] sha1Actual = Hex.toByteArray(IO.collect(remoteFoobarSha1));
		assertTrue(Arrays.equals(sha1Expected, sha1Actual));

		byte[] md5Expected = MD5.digest("overwrite".getBytes()).digest();
		byte[] md5Actual = Hex.toByteArray(IO.collect(remoteFoobarMD5));
		assertTrue(Arrays.equals(md5Expected, md5Actual));

		//
		// Fetch the new one with checksums
		//

		IO.delete(localFoobar.getParentFile());

		assertTrue(repo.fetch("foo/bar", localFoobar));
		assertTrue(localFoobarSha1.isFile());
		assertTrue(localFoobarMD5.isFile());

		assertEquals(IO.collect(localFoobarSha1), IO.collect(remoteFoobarSha1));
		assertEquals(IO.collect(localFoobarMD5), IO.collect(remoteFoobarMD5));

		//
		// Delete the remote entry
		//

		IO.delete(localFoobar);
		assertTrue(repo.delete("foo/bar"));
		assertFalse(remoteFoobar.exists());
		assertFalse(repo.fetch("foo/bar", localFoobar));
		assertFalse(localFoobar.exists());
		Thread.sleep(1000);
		assertFalse(remoteFoobarSha1.exists());
		assertFalse(remoteFoobarMD5.exists());

	}

	public void testChecksumError() throws Exception {
		File localFoobar = IO.getFile(this.local, "some.jar");
		File remoteFoobar = IO.getFile(this.remote, "foo/bar");
		File remoteFoobarSha1 = IO.getFile(this.remote, "foo/bar.sha1");
		File remoteFoobarMD5 = IO.getFile(this.remote, "foo/bar.md5");

		remoteFoobar.getParentFile().mkdirs();
		IO.store("bla", remoteFoobar);
		IO.store("123456", remoteFoobarSha1);
		try {
			repo.fetch("foo/bar", localFoobar);
			fail("Expected an exception because checksum is wrong");
		} catch (Exception e) {
			// ok
		}
	}

	public void testLowercaseChecksum() throws Exception {
		File localFoobar = IO.getFile(this.local, "some.jar");
		File remoteFoobar = IO.getFile(this.remote, "foo/bar");
		File remoteFoobarSha1 = IO.getFile(this.remote, "foo/bar.sha1");
		File remoteFoobarMD5 = IO.getFile(this.remote, "foo/bar.md5");

		remoteFoobar.getParentFile().mkdirs();
		IO.store("bla", remoteFoobar);
		IO.store(" FFA6706FF2127A749973072756F83C532E43ED02\r\n".toLowerCase(), remoteFoobarSha1);

		assertTrue(repo.fetch("foo/bar", localFoobar));
	}

	public void testNoChecksum() throws Exception {
		File localFoobar = IO.getFile(this.local, "some.jar");
		File remoteFoobar = IO.getFile(this.remote, "foo/bar");
		File remoteFoobarSha1 = IO.getFile(this.remote, "foo/bar.sha1");
		File remoteFoobarMD5 = IO.getFile(this.remote, "foo/bar.md5");

		remoteFoobar.getParentFile().mkdirs();
		assertFalse(remoteFoobarSha1.exists());
		assertFalse(remoteFoobarMD5.exists());

		IO.store("bla", remoteFoobar);
		assertTrue(repo.fetch("foo/bar", localFoobar));
	}

	public void testOnlyWrongMD5Checksum() throws Exception {
		File localFoobar = IO.getFile(this.local, "some.jar");
		File remoteFoobar = IO.getFile(this.remote, "foo/bar");
		File remoteFoobarSha1 = IO.getFile(this.remote, "foo/bar.sha1");
		File remoteFoobarMD5 = IO.getFile(this.remote, "foo/bar.md5");

		remoteFoobar.getParentFile().mkdirs();
		assertFalse(remoteFoobarSha1.exists());
		IO.store("1234", remoteFoobarMD5);
		assertTrue(remoteFoobarMD5.exists());

		IO.store("bla", remoteFoobar);
		try {
			assertTrue(repo.fetch("foo/bar", localFoobar));
			fail("Expected exception with a false MD5 checksum");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			//
		}
	}

	public void testOnlyMD5Checksum() throws Exception {
		File localFoobar = IO.getFile(this.local, "some.jar");
		File remoteFoobar = IO.getFile(this.remote, "foo/bar");
		File remoteFoobarSha1 = IO.getFile(this.remote, "foo/bar.sha1");
		File remoteFoobarMD5 = IO.getFile(this.remote, "foo/bar.md5");

		remoteFoobar.getParentFile().mkdirs();
		assertFalse(remoteFoobarSha1.exists());
		IO.store("128ECF542A35AC5270A87DC740918404", remoteFoobarMD5);
		assertTrue(remoteFoobarMD5.exists());

		IO.store("bla", remoteFoobar);
		assertTrue(repo.fetch("foo/bar", localFoobar));
	}
}
