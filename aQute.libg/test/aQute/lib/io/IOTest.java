package aQute.lib.io;

import java.io.File;

import junit.framework.TestCase;

public class IOTest extends TestCase {

	public void testSafeFileName() {
		if (IO.isWindows()) {
			assertEquals("abc%def", IO.toSafeFileName("abc:def"));
			assertEquals("%abc%def%", IO.toSafeFileName("<abc:def>"));
		} else {
			assertEquals("abc%def", IO.toSafeFileName("abc/def"));
			assertEquals("<abc%def>", IO.toSafeFileName("<abc/def>"));
		}
	}

	public void testFilesetCopy() throws Exception {
		File destDir = new File("generated/fileset-copy-test");

		if (destDir.exists()) {
			IO.delete(destDir);
			assertFalse(destDir.exists());
		}

		assertTrue(destDir.mkdirs());

		File srcDir = new File("testresources/fileset");

		IO.copy(srcDir, destDir);

		assertTrue(new File(destDir, "a/b/c/d/e/f/a.abc").exists());
		assertTrue(new File(destDir, "a/b/c/c.abc").exists());
		assertTrue(new File(destDir, "root").exists());
	}

	public void testDestDirIsChildOfSource() throws Exception {
		File parentDir = new File("generated/test/parentDir");

		if (parentDir.exists()) {
			IO.delete(parentDir);
			assertFalse(parentDir.exists());
		}

		assertTrue(parentDir.mkdirs());

		File childDir = new File("generated/test/parentDir/childDir");

		try {
			IO.copy(parentDir, childDir);

			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}
}
