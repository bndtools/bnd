package aQute.lib.fileset;

import java.io.File;

import aQute.lib.io.IO;
import junit.framework.TestCase;

public class FileSetTest extends TestCase {
	File root = new File("testresources/fileset");

	public void testAllABC() {
		FileSet fs = new FileSet(root, "**/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(6, fs.getFiles()
			.size());
	}

	public void testOneA() {
		FileSet fs = new FileSet(root, "*/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	public void testOr() {
		FileSet fs = new FileSet(root, "a/*.abc,a/b/b.abc");
		System.out.println(fs.getFiles());
		assertEquals(2, fs.getFiles()
			.size());
	}

	public void testDirMatch() {
		FileSet fs = new FileSet(root, "*/?/?/**/a.abc");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	public void testAllA() {
		FileSet fs = new FileSet(root, "**/a.*");
		System.out.println(fs.getFiles());
		assertEquals(2, fs.getFiles()
			.size());
	}

	public void testExact() {
		FileSet fs = new FileSet(root, "a/b/c/d/e/f/a.abc");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	public void testSkipIntermediate() {
		FileSet fs = new FileSet(root, "a/**/e/f/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(3, fs.getFiles()
			.size());
	}

	public void testSkipLastDir() {
		FileSet fs = new FileSet(root, "a/b/c/d/e/f/**/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(3, fs.getFiles()
			.size());
	}

	public void testRootAll() {
		FileSet fs = new FileSet(root, "*");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	public void testIncludeA() {
		FileSet fs = new FileSet(root, "**/*");
		assertTrue(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}

	public void testIncludeAWithMultipleWildcards() {
		FileSet fs = new FileSet(root, "a/**/c/**/*");
		assertTrue(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}

	public void testNotA() {
		FileSet fs = new FileSet(root, "a/**/c/**/*.def");
		assertFalse(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}

	public void testNotA2() {
		FileSet fs = new FileSet(root, "a/**/x/**/*.abc");
		assertFalse(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}
}
