package aQute.bnd.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Test;

import aQute.lib.io.IO;

public class TestSignCommand extends TestBndMainBase {

	/**
	 * Test that the sign command shows proper help
	 */
	@Test
	public void testSignCommandHelp() throws Exception {
		executeBndCmd("help", "sign");

		// Check help output contains key elements - help goes to err stream
		String output = getSystemErrContent();
		assertThat(output).contains("Sign JAR files with GPG");
		assertThat(output).contains("--key");
		assertThat(output).contains("--passphrase");
		assertThat(output).contains("--command");
		assertThat(output).contains("--include");
		assertThat(output).contains("--xclude");
		
		expectNoError();
	}

	/**
	 * Test that the sign command with no passphrase and no console gives an error
	 */
	@Test
	public void testSignCommandNoPassphrase() throws Exception {
		// Create a dummy jar file
		File testJar = folder.getFile("test.jar");
		IO.store("dummy content".getBytes(), testJar);

		// Try to sign without passphrase (should fail since there's no console)
		executeBndCmd("sign", testJar.getAbsolutePath());

		// Should have error about missing passphrase
		String errors = getSystemErrContent();
		assertThat(errors).contains("No --passphrase set for PGP key and no console");
	}

	/**
	 * Test that the sign command handles non-existent files properly
	 */
	@Test
	public void testSignCommandNonExistentFile() throws Exception {
		File nonExistent = new File(folder.getRootPath().toFile(), "nonexistent.jar");

		executeBndCmd("sign", "--passphrase", "test", nonExistent.getAbsolutePath());

		// Should have error about file not existing
		String errors = getSystemErrContent();
		assertThat(errors).contains("File or directory does not exist");
	}

	/**
	 * Test that the sign command can scan directories for jar files
	 */
	@Test
	public void testSignCommandScanDirectory() throws Exception {
		// Create test directory structure
		File testDir = folder.getFile("test-sign");
		File subDir1 = new File(testDir, "sub1");
		File subDir2 = new File(testDir, "sub2");
		subDir1.mkdirs();
		subDir2.mkdirs();

		// Create some test jar files
		File jar1 = new File(subDir1, "test1.jar");
		File jar2 = new File(subDir2, "test2.jar");
		File notJar = new File(testDir, "test.txt");
		
		IO.store("jar1".getBytes(), jar1);
		IO.store("jar2".getBytes(), jar2);
		IO.store("txt".getBytes(), notJar);

		// Since we can't actually test GPG signing without a GPG setup,
		// we just verify the command finds the files by checking no unexpected errors
		// and expecting the specific GPG execution error
		executeBndCmd("sign", "--passphrase", "test", "--key", "testkey", testDir.getAbsolutePath());

		// The command should attempt to sign the jars and likely fail at GPG execution
		// but not before finding the files
		String errors = getSystemErrContent();
		// We expect GPG-related errors, not file-not-found errors
		assertThat(errors).doesNotContain("File or directory does not exist");
	}

	/**
	 * Test include/exclude patterns
	 */
	@Test
	public void testSignCommandIncludeExclude() throws Exception {
		// Create test directory structure
		File testDir = folder.getFile("test-patterns");
		File included = new File(testDir, "include-test.jar");
		File excluded = new File(testDir, "exclude-test.jar");
		
		testDir.mkdirs();
		IO.store("included".getBytes(), included);
		IO.store("excluded".getBytes(), excluded);

		// Sign with exclude pattern
		executeBndCmd("sign", "--passphrase", "test", "--key", "testkey", 
			"--xclude", "**/exclude-*.jar", testDir.getAbsolutePath());

		String errors = getSystemErrContent();
		// Should not have file-not-found errors
		assertThat(errors).doesNotContain("File or directory does not exist");
	}
}
