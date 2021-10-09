package aQute.lib.fileset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.lib.io.IO;

public class FileSetTest {
	File root = new File("testresources/fileset");

	@Test
	public void testAllABC() {
		FileSet fs = new FileSet(root, "**/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(6, fs.getFiles()
			.size());
	}

	@Test
	public void testOneA() {
		FileSet fs = new FileSet(root, "*/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	@Test
	public void testOr() {
		FileSet fs = new FileSet(root, "a/*.abc,a/b/b.abc");
		System.out.println(fs.getFiles());
		assertEquals(2, fs.getFiles()
			.size());
	}

	@Test
	public void testDirMatch() {
		FileSet fs = new FileSet(root, "*/?/?/**/a.abc");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	@Test
	public void testAllA() {
		FileSet fs = new FileSet(root, "**/a.*");
		System.out.println(fs.getFiles());
		assertEquals(2, fs.getFiles()
			.size());
	}

	@Test
	public void testExact() {
		FileSet fs = new FileSet(root, "a/b/c/d/e/f/a.abc");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	@Test
	public void testSkipIntermediate() {
		FileSet fs = new FileSet(root, "a/**/e/f/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(3, fs.getFiles()
			.size());
	}

	@Test
	public void testSkipLastDir() {
		FileSet fs = new FileSet(root, "a/b/c/d/e/f/**/*.abc");
		System.out.println(fs.getFiles());
		assertEquals(3, fs.getFiles()
			.size());
	}

	@Test
	public void testRootAll() {
		FileSet fs = new FileSet(root, "*");
		System.out.println(fs.getFiles());
		assertEquals(1, fs.getFiles()
			.size());
	}

	@Test
	public void testIncludeA() {
		FileSet fs = new FileSet(root, "**/*");
		assertTrue(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}

	@Test
	public void testIncludeAWithMultipleWildcards() {
		FileSet fs = new FileSet(root, "a/**/c/**/*");
		assertTrue(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}

	@Test
	public void testNotA() {
		FileSet fs = new FileSet(root, "a/**/c/**/*.def");
		assertFalse(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}

	@Test
	public void testNotA2() {
		FileSet fs = new FileSet(root, "a/**/x/**/*.abc");
		assertFalse(fs.isIncluded(IO.getFile("testresources/fileset/a/b/c/d/e/f/a.abc")));
	}
}
