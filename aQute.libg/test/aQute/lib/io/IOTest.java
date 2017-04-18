package aQute.lib.io;

import java.io.File;
import java.nio.file.Files;

import junit.framework.TestCase;

public class IOTest extends TestCase {

	public void testSafeFileName() {
		if (IO.isWindows()) {
			assertEquals("abc%def", IO.toSafeFileName("abc:def"));
			assertEquals("%abc%def%", IO.toSafeFileName("<abc:def>"));
			assertEquals("LPT1_", IO.toSafeFileName("LPT1"));
			assertEquals("COM2_", IO.toSafeFileName("COM2"));
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

		IO.mkdirs(destDir);
		assertTrue(destDir.isDirectory());

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

		IO.mkdirs(parentDir);
		assertTrue(parentDir.isDirectory());

		File childDir = new File("generated/test/parentDir/childDir");

		try {
			IO.copy(parentDir, childDir);

			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testIfCreateSymlinkOrCopyFileDependingOnOS() throws Exception {
		File link = new File("generated/test/target.dat");

		IO.delete(link);

		assertFalse(link.exists() || IO.isSymbolicLink(link));

		IO.mkdirs(link.getParentFile());

		File source = new File("testresources/zipped.dat");

		assertTrue(source.exists());

		assertTrue(IO.createSymbolicLinkOrCopy(link, source));

		if (IO.isWindows()) {
			assertFalse(IO.isSymbolicLink(link));
		} else {
			assertTrue(IO.isSymbolicLink(link));
		}
	}

	public void testOnlyCopyIfReallyNeededOnWindows() throws Exception {
		if (IO.isWindows()) {
			File link = new File("generated/test/target.dat");

			IO.delete(link);

			assertFalse(link.exists() || IO.isSymbolicLink(link));

			IO.mkdirs(link.getParentFile());

			File source = new File("testresources/zipped.dat");
			assertTrue(source.exists());

			assertTrue(IO.createSymbolicLinkOrCopy(link, source));

			assertEquals(link.lastModified(), source.lastModified());
			assertEquals(link.length(), source.length());

			assertTrue(IO.createSymbolicLinkOrCopy(link, source));

			assertEquals(link.lastModified(), source.lastModified());
			assertEquals(link.length(), source.length());
		}
	}

	public void testCreateSymlinkOrCopyWillDeleteOriginalLink() throws Exception {
		File originalSource = new File("testresources/unzipped.dat");
		File link = new File("generated/test/originalLink");

		IO.delete(link);

		assertFalse(IO.isSymbolicLink(link));

		assertTrue(IO.createSymbolicLinkOrCopy(link, originalSource));

		File newSource = new File("testresources/zipped.dat");

		assertTrue(IO.createSymbolicLinkOrCopy(link, newSource));

		if (IO.isWindows()) {
			assertEquals(link.lastModified(), newSource.lastModified());
			assertEquals(link.length(), newSource.length());
		} else {
			assertTrue(IO.isSymbolicLink(link));
			assertTrue(Files.readSymbolicLink(link.toPath()).equals(newSource.toPath()));
		}
	}
}
