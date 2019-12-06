package aQute.lib.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;

import aQute.lib.io.IO.EnvironmentCalculator;

public class IOTest {

	private Path tmp;

	@BeforeEach
	public void before(TestInfo info) throws Exception {
		Method testMethod = info.getTestMethod()
			.get();
		tmp = Paths.get("generated/tmp/test", getClass().getName(), testMethod.getName())
			.toAbsolutePath();
		IO.delete(tmp);
		IO.mkdirs(tmp);
	}

	@Test
	public void testEnvVarsForHome() throws Exception {
		Map<String, String> map = new HashMap<>();

		EnvironmentCalculator ec = new IO.EnvironmentCalculator(false) {
			@Override
			String getenv(String key) {
				return map.getOrDefault(key, System.getenv(key));
			}
		};

		assertEquals(new File(System.getProperty("user.home")), ec.getHome());
		assertEquals(new File(System.getProperty("user.home")), IO.home);

		File dir = IO.getFile("generated");
		map.put("HOME", dir.getAbsolutePath());
		assertEquals(dir, ec.getHome());

		EnvironmentCalculator ec2 = new IO.EnvironmentCalculator(true) {
			@Override
			String getenv(String key) {
				return map.getOrDefault(key, System.getenv(key));
			}
		};
		map.put("SystemDrive", "C:");
		map.put("username", "foobar");
		map.put("userprofile", "%SystemDrive%\\Documents and Settings\\%username%");
		map.put("HOME", "%userprofile%");

		// cannot use file system since this might not be windows
		assertEquals("C:\\Documents and Settings\\foobar", ec2.getSystemEnv("HOME"));
	}

	@Test
	@DisabledOnOs(WINDOWS)
	public void testSafeFileName() {
		assertEquals("abc%def", IO.toSafeFileName("abc/def"));
		assertEquals("<abc%def>", IO.toSafeFileName("<abc/def>"));
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void testSafeFileNameWindows() {
		assertEquals("abc%def", IO.toSafeFileName("abc:def"));
		assertEquals("%abc%def%", IO.toSafeFileName("<abc:def>"));
		assertEquals("LPT1_", IO.toSafeFileName("LPT1"));
		assertEquals("COM2_", IO.toSafeFileName("COM2"));
	}

	@Test
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

	@Test
	public void testCopyURLToByteArray() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		byte[] result = IO.read(src.toURI()
			.toURL());
		assertEquals((int) src.length(), result.length);
		assertEquals(file.length, result.length);
		int length = file.length;
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], result[i]);
		}
	}

	@Test
	public void testCopyToExactHeapByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocate((int) src.length()));
		assertEquals((int) src.length(), bb.position());
		assertEquals(bb.capacity(), bb.position());
		assertFalse(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToSmallerHeapByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocate((int) src.length() - 8));
		assertEquals(bb.capacity(), bb.position());
		assertFalse(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToLargerHeapByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocate((int) src.length() + 20));
		assertEquals((int) src.length(), bb.position());
		assertTrue(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToExactDirectByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocateDirect((int) src.length()));
		assertEquals((int) src.length(), bb.position());
		assertEquals(bb.capacity(), bb.position());
		assertFalse(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToSmallerDirectByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocateDirect((int) src.length() - 8));
		assertEquals(bb.capacity(), bb.position());
		assertFalse(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToLargerDirectByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocateDirect((int) src.length() + 20));
		assertEquals((int) src.length(), bb.position());
		assertTrue(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToHugeDirectByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		ByteBuffer bb = IO.copy(IO.stream(src), ByteBuffer.allocateDirect(IOConstants.PAGE_SIZE * 32));
		assertEquals((int) src.length(), bb.position());
		assertTrue(bb.hasRemaining());
		bb.flip();
		int length = bb.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], bb.get());
		}
	}

	@Test
	public void testCopyToOffsetHeapByteBuffer() throws Exception {
		File src = new File("testresources/unzipped.dat");
		byte[] file = IO.read(src);
		byte[] wrapped = new byte[file.length + 1];
		ByteBuffer bb = ByteBuffer.wrap(wrapped);
		bb.put((byte) 0xbb);
		ByteBuffer slice = bb.slice();
		IO.copy(IO.stream(src), slice);
		assertEquals(wrapped.length, slice.arrayOffset() + slice.position());
		assertFalse(slice.hasRemaining());
		int length = wrapped.length;
		assertEquals((byte) 0xbb, wrapped[0]);
		for (int i = 1; i < length; i++) {
			assertEquals(file[i - 1], wrapped[i]);
		}
		slice.flip();
		length = slice.remaining();
		for (int i = 0; i < length; i++) {
			assertEquals(file[i], slice.get());
		}
	}

	@Test
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
		} catch (IllegalArgumentException e) {}
	}

	@Test
	public void testIfCreateSymlinkOrCopyFileDependingOnOS() throws Exception {
		File link = new File("generated/test/target.dat");

		IO.delete(link);

		assertFalse(link.exists() || IO.isSymbolicLink(link));

		IO.mkdirs(link.getParentFile());

		File source = new File("testresources/zipped.dat");

		assertTrue(source.exists());

		assertTrue(IO.createSymbolicLinkOrCopy(link, source));

		assertThat(IO.isSymbolicLink(link)).isNotEqualTo(IO.isWindows());
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void testOnlyCopyIfReallyNeededOnWindows() throws Exception {
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

	@Test
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
			assertTrue(Files.readSymbolicLink(link.toPath())
				.equals(newSource.toPath()));
		}
	}

	@Test
	public void testCreateDirectory_Symlink() throws Exception {
		Path rootDirectory = tmp;

		Path target = Files.createDirectories(rootDirectory.resolve("target")
			.toAbsolutePath());
		assertTrue(target.toFile()
			.exists());

		Path link = Paths.get(rootDirectory.toAbsolutePath()
			.toString(), "link");
		Path symbolicLink = Files.createSymbolicLink(link, target);
		assertTrue(IO.isSymbolicLink(symbolicLink));

		IO.mkdirs(symbolicLink);
		assertTrue(symbolicLink.toFile()
			.exists());
	}

	@Test
	public void testCreateDirectory_SymlinkMissingTarget() throws Exception {
		Path rootDirectory = tmp;

		Path target = rootDirectory.resolve("target")
			.toAbsolutePath();
		assertFalse(target.toFile()
			.exists());

		Path link = Paths.get(rootDirectory.toAbsolutePath()
			.toString(), "link");
		Path symbolicLink = Files.createSymbolicLink(link, target);
		assertTrue(IO.isSymbolicLink(symbolicLink));

		IO.mkdirs(symbolicLink);
		assertTrue(symbolicLink.toFile()
			.exists());
	}

	public void testCollectEncoded() throws Exception {
		InputStream in = IO.stream("testString", "UTF-8");
		String result = IO.collect(in, "UTF-8");
		assertEquals("testString", result);
	}

}
