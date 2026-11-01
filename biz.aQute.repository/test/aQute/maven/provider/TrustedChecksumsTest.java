package aQute.maven.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.maven.api.Archive;
import aQute.maven.provider.TrustedChecksums.ArtifactChecksum;
import aQute.maven.provider.TrustedChecksums.ChecksumPolicy;
import aQute.maven.provider.TrustedChecksums.TrustedChecksumException;

public class TrustedChecksumsTest {
	@Test
	public void testParseValidChecksum() throws Exception {
	    String line = "org.example:artifact:1.0=sha256:abc123def456";
	    ArtifactChecksum ac = ArtifactChecksum.parse(line);
	    assertEquals(Archive.valueOf("org.example:artifact:1.0"), ac.archive());
	    assertEquals("sha256", ac.hashType());
	    assertEquals("abc123def456", ac.hash());
	}

	@Test
	public void testParseValidChecksum2() throws Exception {
		// check if trimming works correctly
		String source = " #comment line \n" + " org.example:artifact:1.0 = sha256 : abc123def456  ";
		List<ArtifactChecksum> list = TrustedChecksums.read(source);
		assertThat(list).hasSize(1);

		ArtifactChecksum ac = list.get(0);
		assertEquals(Archive.valueOf("org.example:artifact:1.0"), ac.archive());
		assertEquals("sha256", ac.hashType());
		assertEquals("abc123def456", ac.hash());
	}

	@Test
	public void testParseInvalidFormat() throws Exception {
	    String invalidLine = "invalid_format_no_equals";
	    assertThrows(IllegalArgumentException.class, () ->
	        ArtifactChecksum.parse(invalidLine)
	    );
	}

	@Test
	public void testChecksumPolicyFromString() throws Exception {
		assertEquals(ChecksumPolicy.LENIENT, ChecksumPolicy.fromString("lenient"));
		assertEquals(ChecksumPolicy.STRICT, ChecksumPolicy.fromString("strict"));
		assertEquals(ChecksumPolicy.LENIENT, ChecksumPolicy.fromString(""));
		assertEquals(ChecksumPolicy.LENIENT, ChecksumPolicy.fromString(null));
		assertEquals(ChecksumPolicy.LENIENT, ChecksumPolicy.fromString("  lenient  "));
		assertEquals(ChecksumPolicy.STRICT, ChecksumPolicy.fromString("  STRICT  "));
	}

	@Test
	public void testChecksumPolicyFromStringInvalid() throws Exception {
		assertThrows(IllegalArgumentException.class, () ->
			ChecksumPolicy.fromString("invalid")
		);
	}

	@Test
	public void testLenientModeNoChecksumsFile(@TempDir File tmpDir) throws Exception {
		// In lenient mode, if there's no checksums file, it should return false (not throw)
		File nonExistentFile = new File(tmpDir, "nonexistent.checksums");
		TrustedChecksums tc = new TrustedChecksums(nonExistentFile, ChecksumPolicy.LENIENT);
		tc.open();

		Archive archive = Archive.valueOf("org.example:artifact:1.0");
		File dummyFile = new File(tmpDir, "dummy.jar");
		Files.write(dummyFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

		// Should not throw, should return false
		boolean result = tc.checkTrustedChecksum(archive, dummyFile);
		assertEquals(false, result);
	}

	@Test
	public void testStrictModeNoChecksumsFile(@TempDir File tmpDir) throws Exception {
		// In strict mode, if there's no checksums file, it should throw
		File nonExistentFile = new File(tmpDir, "nonexistent.checksums");
		TrustedChecksums tc = new TrustedChecksums(nonExistentFile, ChecksumPolicy.STRICT);
		tc.open();

		Archive archive = Archive.valueOf("org.example:artifact:1.0");
		File dummyFile = new File(tmpDir, "dummy.jar");
		Files.write(dummyFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

		// Should throw
		assertThrows(TrustedChecksumException.class, () ->
			tc.checkTrustedChecksum(archive, dummyFile)
		);
	}

	@Test
	public void testLenientModeMissingEntry(@TempDir File tmpDir) throws Exception {
		// In lenient mode, if a dependency is missing from checksums, it should return false (not throw)
		File checksumsFile = new File(tmpDir, "test.checksums");
		String checksumContent = "org.example:other:1.0=sha256:abc123def456\n";
		Files.write(checksumsFile.toPath(), checksumContent.getBytes(StandardCharsets.UTF_8));

		TrustedChecksums tc = new TrustedChecksums(checksumsFile, ChecksumPolicy.LENIENT);
		tc.open();

		Archive archive = Archive.valueOf("org.example:artifact:1.0");
		File dummyFile = new File(tmpDir, "dummy.jar");
		Files.write(dummyFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

		// Should not throw, should return false
		boolean result = tc.checkTrustedChecksum(archive, dummyFile);
		assertEquals(false, result);
	}

	@Test
	public void testStrictModeMissingEntry(@TempDir File tmpDir) throws Exception {
		// In strict mode, if a dependency is missing from checksums, it should throw
		File checksumsFile = new File(tmpDir, "test.checksums");
		String checksumContent = "org.example:other:1.0=sha256:abc123def456\n";
		Files.write(checksumsFile.toPath(), checksumContent.getBytes(StandardCharsets.UTF_8));

		TrustedChecksums tc = new TrustedChecksums(checksumsFile, ChecksumPolicy.STRICT);
		tc.open();

		Archive archive = Archive.valueOf("org.example:artifact:1.0");
		File dummyFile = new File(tmpDir, "dummy.jar");
		Files.write(dummyFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

		// Should throw
		TrustedChecksumException ex = assertThrows(TrustedChecksumException.class, () ->
			tc.checkTrustedChecksum(archive, dummyFile)
		);
		assertThat(ex.getMessage()).contains("Strict checksum policy").contains("no checksum entry found");
	}

	@Test
	public void testStrictModeWrongVersion(@TempDir File tmpDir) throws Exception {
		// In strict mode, if the version doesn't match, it should throw
		File checksumsFile = new File(tmpDir, "test.checksums");
		String checksumContent = "org.example:artifact:1.33=sha256:abc123def456\n";
		Files.write(checksumsFile.toPath(), checksumContent.getBytes(StandardCharsets.UTF_8));

		TrustedChecksums tc = new TrustedChecksums(checksumsFile, ChecksumPolicy.STRICT);
		tc.open();

		// Trying to verify version 1.37 when checksums has 1.33
		Archive archive = Archive.valueOf("org.example:artifact:1.37");
		File dummyFile = new File(tmpDir, "dummy.jar");
		Files.write(dummyFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

		// Should throw
		TrustedChecksumException ex = assertThrows(TrustedChecksumException.class, () ->
			tc.checkTrustedChecksum(archive, dummyFile)
		);
		assertThat(ex.getMessage()).contains("Strict checksum policy").contains("no checksum entry found");
	}

	@Test
	public void testLenientModeWrongVersion(@TempDir File tmpDir) throws Exception {
		// In lenient mode, if the version doesn't match, it should return false (not throw)
		File checksumsFile = new File(tmpDir, "test.checksums");
		String checksumContent = "org.example:artifact:1.33=sha256:abc123def456\n";
		Files.write(checksumsFile.toPath(), checksumContent.getBytes(StandardCharsets.UTF_8));

		TrustedChecksums tc = new TrustedChecksums(checksumsFile, ChecksumPolicy.LENIENT);
		tc.open();

		// Trying to verify version 1.37 when checksums has 1.33
		Archive archive = Archive.valueOf("org.example:artifact:1.37");
		File dummyFile = new File(tmpDir, "dummy.jar");
		Files.write(dummyFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

		// Should not throw, should return false
		boolean result = tc.checkTrustedChecksum(archive, dummyFile);
		assertEquals(false, result);
	}
}

