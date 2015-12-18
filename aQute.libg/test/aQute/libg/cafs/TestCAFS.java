package aQute.libg.cafs;

import java.io.File;

import junit.framework.TestCase;

public class TestCAFS extends TestCase {
	File tmp;

	@Override
	public void setUp() throws Exception {
		tmp = new File(System.getProperty("java.io.tmpdir"));
		assertTrue(tmp.isDirectory());
	}

	public void testX() {}

	// public void testSimple() throws Exception {
	// File dir = new File(tmp, getClass().getName() +
	// System.currentTimeMillis());
	// CAFS cafs = new CAFS(dir, true);
	// MessageDigest digester = MessageDigest.getInstance("SHA-1");
	//
	// DigestInputStream dis = new
	// DigestInputStream(getClass().getResourceAsStream(
	// "file1.txt"), digester);
	// SHA1 sha1 = cafs.write(dis);
	//
	// assertEquals(new SHA1(digester.digest()), sha1);
	//
	// digester.reset();
	// dis = new
	// DigestInputStream(getClass().getResourceAsStream("file1-1.txt"),
	// digester);
	//
	// SHA1 sha1_1 = cafs.write(dis);
	// assertEquals(sha1, sha1_1);
	// assertEquals( new SHA1(digester.digest()), sha1_1);
	//
	// SHA1 sha2 = cafs.write(getClass().getResourceAsStream("file2.txt"));
	//
	// assertFalse( sha1.equals(sha2));
	//
	// int n = 0;
	// for (SHA1 key : cafs) {
	// assertTrue(sha1.equals(key) || sha2.equals(key));
	// System.err.println(n++ + ": " + key);
	// InputStream in = cafs.read(key);
	// assertNotNull(in);
	// String s = IO.collect(in, "UTF-8");
	// byte[] b = s.getBytes("UTF-8");
	// MessageDigest md = MessageDigest.getInstance("SHA1");
	// md.update(b);
	// assertEquals(key, new SHA1(md.digest()));
	// }
	//
	// // Close out db and reopen it again.
	// cafs.close();
	// cafs = new CAFS(dir, false);
	//
	// for (SHA1 key : cafs) {
	// assertTrue(sha1.equals(key) || sha2.equals(key));
	// }
	//
	// InputStream in = cafs.read(sha2);
	// assertNotNull(in);
	// SHA1 rsha2 = SHA1.getDigester().from(in);
	//
	// assertEquals(sha2, rsha2);
	//
	// cafs.close();
	// }
}
