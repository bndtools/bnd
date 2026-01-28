package aQute.bnd.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

/**
 * Tests for the xref command, particularly the --nested flag for analyzing
 * nested JARs referenced via Bundle-ClassPath.
 */
public class TestXRefCommand extends TestBndMainBase {

	/**
	 * Test xref command without --nested flag. Should only show classes from
	 * main JAR.
	 */
	@Test
	public void testXRefWithoutNested() throws Exception {
		File testBundle = createTestBundleWithNestedJar();

		executeBndCmd("xref", testBundle.getAbsolutePath());

		String output = getSystemOutContent();

		// Should contain main package (org.osgi.framework from main JAR)
		assertThat(output).contains("org.osgi.framework");

		// Should NOT contain nested package (since --nested is not used)
		// The nested JAR contains org.objectweb.asm classes
		assertThat(output).doesNotContain("org.objectweb.asm");

		expectNoError();
	}

	/**
	 * Test xref command with --nested flag. Should show classes from both main
	 * JAR and nested JARs.
	 */
	@Test
	public void testXRefWithNested() throws Exception {
		File testBundle = createTestBundleWithNestedJar();

		executeBndCmd("xref", "--nested", testBundle.getAbsolutePath());

		String output = getSystemOutContent();

		// Should contain main package
		assertThat(output).contains("org.osgi.framework");

		// Should contain nested package (since --nested is used)
		assertThat(output).contains("org.objectweb.asm");

		expectNoError();
	}

	/**
	 * Test xref command with --nested flag on bundle without Bundle-ClassPath.
	 * Should work normally without errors.
	 */
	@Test
	public void testXRefNestedWithoutBundleClassPath() throws Exception {
		File testJar = IO.getFile("../biz.aQute.bndlib.tests/jar/osgi.core-4.3.0.jar");

		executeBndCmd("xref", "--nested", testJar.getAbsolutePath());

		String output = getSystemOutContent();

		// Should contain the osgi packages
		assertThat(output).contains("org.osgi");

		expectNoError();
	}

	/**
	 * Test xref command with --nested and --classes flags. Should show
	 * individual class names from both main and nested JARs.
	 */
	@Test
	public void testXRefNestedWithClasses() throws Exception {
		File testBundle = createTestBundleWithNestedJar();

		executeBndCmd("xref", "--nested", "--classes", testBundle.getAbsolutePath());

		String output = getSystemOutContent();

		// Should contain classes from main JAR
		assertThat(output).contains("org.osgi.framework");

		// Should contain classes from nested JAR
		assertThat(output).contains("org.objectweb.asm");

		expectNoError();
	}

	/**
	 * Creates a test bundle with a nested JAR referenced via Bundle-ClassPath.
	 * Uses real JAR files from the test resources.
	 * 
	 * Structure:
	 * - Classes from osgi.core jar (in main JAR)
	 * - lib/asm.jar containing ASM classes
	 * - Bundle-ClassPath: .,lib/asm.jar
	 */
	private File createTestBundleWithNestedJar() throws Exception {
		// Get existing JARs from test resources (paths relative to biz.aQute.bnd working directory)
		File asmJar = IO.getFile("../biz.aQute.bndlib.tests/jar/asm.jar");
		File osgiJar = IO.getFile("../biz.aQute.bndlib.tests/jar/osgi.core.jar");

		// Create a bundle with Bundle-ClassPath
		try (Builder builder = new Builder()) {
			builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, "test.xref.bundle");
			builder.setProperty(Constants.BUNDLE_VERSION, "1.0.0");
			builder.setProperty(Constants.BUNDLE_CLASSPATH, ".,lib/asm.jar");
			builder.setProperty("-includeresource", 
				"@" + osgiJar.getAbsolutePath() + "!/org/osgi/framework/**," +
				"lib/asm.jar=" + asmJar.getAbsolutePath());

			Jar jar = builder.build();
			assertTrue(builder.check());

			// Verify the bundle was created correctly
			assertThat(jar.getResource("lib/asm.jar")).isNotNull();
			assertThat(jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH))
				.isEqualTo(".,lib/asm.jar");

			File bundleFile = folder.getFile("test-xref-bundle.jar");
			jar.write(bundleFile);
			return bundleFile;
		}
	}
}
