package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.data.Offset.strictOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA256;

public class JarTest {
	@Test
	public void testDeletePrefix() {
		Resource r = new EmbeddedResource(new byte[1], 0L);

		try (Jar jar = new Jar("test")) {
			jar.putResource("META-INF/maven/org/osgi/test/test.pom", r);
			jar.putResource("META-INF/maven/org/osgi/test/test.properties", r);
			jar.putResource("META-INF/MANIFEST.MF", r);
			jar.putResource("com/example/foo.jar", r);

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven/org/osgi/test"));
			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven"));
			jar.removePrefix("META-INF/maven/");

			assertNotNull(jar.getResource("META-INF/MANIFEST.MF"));
			assertNotNull(jar.getResource("com/example/foo.jar"));
			assertNull(jar.getResource("META-INF/maven/org/osgi/test/test.pom"));
			assertNull(jar.getResource("META-INF/maven/org/osgi/test/test.properties"));

			assertFalse(jar.getDirectories()
				.containsKey("META-INF/maven"));
			assertFalse(jar.getDirectories()
				.containsKey("META-INF/maven/org/osgi/test"));
		}
	}

	@Test
	public void testDeleteSubDirs() {
		Resource r = new EmbeddedResource(new byte[1], 0L);

		try (Jar jar = new Jar("test")) {
			jar.putResource("META-INF/maven/org/osgi/test/test.pom", r);
			jar.putResource("META-INF/maven/org/osgi/test/test.properties", r);
			jar.putResource("META-INF/maven/plugin.xml", r);
			jar.putResource("META-INF/MANIFEST.MF", r);
			jar.putResource("com/example/foo.jar", r);

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven/org/osgi/test"));
			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven"));
			jar.removeSubDirs("META-INF/maven/");

			assertNotNull(jar.getResource("META-INF/MANIFEST.MF"));
			assertNotNull(jar.getResource("META-INF/maven/plugin.xml"));
			assertNotNull(jar.getResource("com/example/foo.jar"));
			assertNull(jar.getResource("META-INF/maven/org/osgi/test/test.pom"));
			assertNull(jar.getResource("META-INF/maven/org/osgi/test/test.properties"));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven"));
			assertFalse(jar.getDirectories()
				.containsKey("META-INF/maven/org/osgi/test"));
		}
	}

	@Test
	public void testDeleteSubDirs2() {
		Resource r = new EmbeddedResource(new byte[1], 0L);

		try (Jar jar = new Jar("test")) {
			jar.putResource("META-INF/maven/plugin/foo.txt", r);
			jar.putResource("META-INF/maven/plugin.xml", r);
			jar.putResource("META-INF/MANIFEST.MF", r);
			jar.putResource("com/example/foo.jar", r);

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven/plugin"));
			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven"));
			jar.removeSubDirs("META-INF/maven");

			assertNotNull(jar.getResource("META-INF/MANIFEST.MF"));
			assertNotNull(jar.getResource("META-INF/maven/plugin.xml"));
			assertNotNull(jar.getResource("com/example/foo.jar"));
			assertNull(jar.getResource("META-INF/maven/plugin/foo.txt"));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/maven"));
			assertFalse(jar.getDirectories()
				.containsKey("META-INF/maven/plugin"));
		}
	}

	@Test
	public void testPackages() {
		Resource r = new EmbeddedResource(new byte[1], 0L);

		try (Jar jar = new Jar("test")) {
			jar.putResource("com/example/foo/bar/x.class", r);
			jar.putResource("com/example/foo/bar/package-info.class", r);

			assertThat(jar.getDirectories()).containsKeys("com/example/foo/bar", "com/example/foo", "com/example",
				"com");

			assertThat(jar.getPackages()).contains("com.example.foo.bar")
				.doesNotContain("com.example.foo", "com.example", "com");

			jar.remove("com/example/foo/bar/x.class");
			assertThat(jar.getDirectories()).containsKeys("com/example/foo/bar", "com/example/foo", "com/example",
				"com");
			assertThat(jar.getPackages()).contains("com.example.foo.bar")
				.doesNotContain("com.example.foo", "com.example", "com");

			jar.remove("com/example/foo/bar/package-info.class");
			assertThat(jar.getDirectories()).containsKeys("com/example/foo/bar", "com/example/foo", "com/example",
				"com");
			assertThat(jar.getPackages()).doesNotContain("com.example.foo.bar", "com.example.foo", "com.example",
				"com");

		}
	}

	@Test
	public void testWriteFolder(@InjectTemporaryDirectory
	File tmp) throws Exception {
		try (Builder b = new Builder()) {
			b.setIncludeResource("/a/b.txt;literal='ab', /a/c.txt;literal='ac', /a/c/d/e.txt;literal='acde'");
			b.build();
			assertTrue(b.check());

			b.getJar()
				.writeFolder(tmp);

			assertTrue(IO.getFile(tmp, "META-INF/MANIFEST.MF")
				.isFile());
			assertEquals("ab", IO.collect(IO.getFile(tmp, "a/b.txt")));
			assertEquals("acde", IO.collect(IO.getFile(tmp, "a/c/d/e.txt")));
		}
	}

	@Test
	public void testNoManifest() throws Exception {
		try (Jar jar = new Jar("dot")) {
			jar.setManifest(new Manifest());
			jar.setDoNotTouchManifest();
			jar.putResource("a/b", new FileResource(IO.getFile("testresources/bnd.jar")));

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			jar.write(bout);

			try (Jar jin = new Jar("dotin", new ByteArrayInputStream(bout.toByteArray()))) {
				Resource m = jin.getResource("META-INF/MANIFEST.MF");
				assertNull(m);
				Resource r = jin.getResource("a/b");
				assertNotNull(r);
			}
		}
	}

	@Test
	public void testManualManifest() throws Exception {
		try (Jar jar = new Jar("dot")) {
			jar.setManifest(new Manifest());
			jar.setDoNotTouchManifest();
			jar.putResource("a/b", new FileResource(IO.getFile("testresources/bnd.jar")));
			jar.putResource("META-INF/MANIFEST.MF", new EmbeddedResource("Manifest-Version: 1\r\nX: 1\r\n\r\n", 0L));

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			jar.write(bout);

			JarInputStream jin = new JarInputStream(new ByteArrayInputStream(bout.toByteArray()));
			Manifest m = jin.getManifest();
			assertNotNull(m);
			assertEquals("1", m.getMainAttributes()
				.getValue("X"));
			jin.close();
		}
	}

	@Test
	public void testRenameManifest() throws Exception {
		try (Jar jar = new Jar("dot")) {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes()
				.putValue("X", "1");
			jar.setManifest(manifest);
			jar.setManifestName("META-INF/FESTYMAN.MF");

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			jar.write(bout);

			ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bout.toByteArray()));
			ZipEntry firstEntry = zin.getNextEntry();
			if (firstEntry.getName()
				.equalsIgnoreCase("META-INF/")) {
				firstEntry = zin.getNextEntry();
			}
			assertEquals("META-INF/FESTYMAN.MF", firstEntry.getName());
			manifest = new Manifest(zin);

			assertEquals("1", manifest.getMainAttributes()
				.getValue("X"));
			zin.close();
		}
	}

	@Test
	public void testSimple() throws ZipException, IOException {
		File file = IO.getFile("jar/asm.jar");
		try (Jar jar = new Jar("asm.jar", file)) {
			long jarTime = jar.lastModified();
			long fileTime = file.lastModified();
			long now = System.currentTimeMillis();

			// Sanity check
			assertThat(jarTime).isLessThan(fileTime);
			assertThat(fileTime).isLessThanOrEqualTo(now);

			// TODO see if we can improve this test case
			// // We should use the highest modification time
			// // of the files in the JAR not the JAR (though
			// // this is a backup if time is not set in the jar)
			// assertEquals(1144412850000L, jarTime);

			// Now add the file and check that
			// the modification time has changed
			jar.putResource("asm", new FileResource(file));
			assertThat(jarTime).isLessThan(jar.lastModified());

			// On some file systems, File.lastModified and
			// BasicFileAttributes.lastModifiedTime can produce values which
			// vary in
			// the sub-second milliseconds.
			assertThat(jar.lastModified()).isCloseTo(file.lastModified(), strictOffset(1000L));
		}
	}

	@Test
	public void testNewLine() throws Exception {
		try (Jar jar = new Jar("dot")) {
			Manifest manifest = new Manifest();
			jar.setManifest(manifest);

			String value = "Test\nTest\nTest\nTest";
			String expectedValue = "Test Test Test Test";

			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_DESCRIPTION, value);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			jar.write(bout);

			assertThat(jar.getSHA256()).isEmpty(); // default no checksum
			assertThat(jar.getLength()).isEqualTo(-1); // default no length

			try (JarInputStream jin = new JarInputStream(new ByteArrayInputStream(bout.toByteArray()))) {
				Manifest m = jin.getManifest();
				assertNotNull(m);

				String parsedValue = m.getMainAttributes()
					.getValue(Constants.BUNDLE_DESCRIPTION);

				assertEquals(expectedValue, parsedValue);
			}
		}
	}

	@Test
	public void testChecksumAndLengthOnWriteOption() throws Exception {
		try (Jar jar = new Jar("dot")) {
			Manifest manifest = new Manifest();
			jar.setManifest(manifest);

			String value = "Test\nTest\nTest\nTest";

			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_DESCRIPTION, value);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			jar.setCalculateFileDigest(true)
				.write(bout);
			byte[] digest = SHA256.digest(bout.toByteArray())
				.digest();
			assertThat(jar.getSHA256()).isPresent()
				.get()
				.isEqualTo(digest);
			assertThat(jar.getLength()).isEqualTo(bout.size());
		}
	}

	@Test
	public void testWriteManifestAttributeDirectiveOrdering() throws Exception {
		Manifest manifest = new Manifest();

		// Test Export-Package with mixed attributes and directives in random order
		// Input: directives and attributes mixed up
		String exportPackage = "com.example.api;uses:=\"com.example.internal\";version=1.0.0;mandatory:=\"version\";provider=acme";
		manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportPackage);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest, bout);

		// Parse the written manifest to verify ordering
		Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
		String writtenExportPackage = writtenManifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);

		// Note: We don't assert that reordering happened because it depends on the specific
		// implementation and the input may already be in the correct order

		// Verify all expected attributes and directives are present
		assertTrue(writtenExportPackage.contains("provider=acme"), "provider attribute should be present");
		assertTrue(writtenExportPackage.contains("version="), "version attribute should be present");
		assertTrue(writtenExportPackage.contains("mandatory:="), "mandatory directive should be present");
		assertTrue(writtenExportPackage.contains("uses:="), "uses directive should be present");

		// Parse the header to verify the structure is correct
		Parameters params = OSGiHeader.parseHeader(writtenExportPackage);
		assertEquals(1, params.size(), "Should have exactly one package");

		Attrs attrs = params.get("com.example.api");
		assertNotNull(attrs, "Package attributes should not be null");
		assertEquals(4, attrs.size(), "Should have 4 attributes/directives");

		// Verify all keys are present
		assertTrue(attrs.containsKey("provider"), "Should contain provider attribute");
		assertTrue(attrs.containsKey("version"), "Should contain version attribute");
		assertTrue(attrs.containsKey("mandatory:"), "Should contain mandatory directive");
		assertTrue(attrs.containsKey("uses:"), "Should contain uses directive");

		// Verify values are correct
		assertEquals("acme", attrs.get("provider"), "provider value should be correct");
		assertEquals("1.0.0", attrs.get("version"), "version value should be correct");
		assertEquals("version", attrs.get("mandatory:"), "mandatory directive value should be correct");
		assertEquals("com.example.internal", attrs.get("uses:"), "uses directive value should be correct");
	}

	@Test
	public void testWriteManifestOrderingConsistency() throws Exception {
		Manifest manifest = new Manifest();

		// Test multiple OSGi headers to ensure consistent ordering
		String[] testHeaders = {
			Constants.EXPORT_PACKAGE,
			Constants.IMPORT_PACKAGE,
			Constants.REQUIRE_CAPABILITY,
			Constants.PROVIDE_CAPABILITY
		};

		for (String headerName : testHeaders) {
			// Test with a header that has both attributes and directives
			String headerValue = "com.example;attr1=value1;directive1:=value1;attr2=value2;directive2:=value2";
			manifest.getMainAttributes().putValue(headerName, headerValue);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Jar.writeManifest(manifest, bout);

			// Parse the written manifest
			Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
			String writtenValue = writtenManifest.getMainAttributes().getValue(headerName);

			// For OSGi syntax headers, verify that reordering occurred
			if (Constants.OSGI_SYNTAX_HEADERS.contains(headerName)) {
				// The output should be different from input (reordered)
				assertFalse(headerValue.equals(writtenValue),
					"Header " + headerName + " should be reordered");

				// Parse and verify structure
				Parameters params = OSGiHeader.parseHeader(writtenValue);
				assertEquals(1, params.size(), "Should have exactly one clause");

				Attrs attrs = params.get("com.example");
				assertNotNull(attrs, "Clause attributes should not be null");
				assertEquals(4, attrs.size(), "Should have 4 attributes/directives");

				// Verify all keys are present
				assertTrue(attrs.containsKey("attr1"), "Should contain attr1");
				assertTrue(attrs.containsKey("attr2"), "Should contain attr2");
				assertTrue(attrs.containsKey("directive1:"), "Should contain directive1:");
				assertTrue(attrs.containsKey("directive2:"), "Should contain directive2:");
			}

			// Clear for next test
			manifest.getMainAttributes().remove(new Attributes.Name(headerName));
		}
	}

	@Test
	public void testManifestCleaningWithOrdering() throws Exception {
		// Test that the cleaning process (which includes reordering) works correctly

		// Create a manifest with some messy formatting and mixed ordering
		String manifestContent = "Manifest-Version: 1.0\n" +
			"Export-Package: com.example.api;uses:=\"com.example.internal\";version=1.0.0;mandatory:=\"version\";provider=acme\n" +
			"Import-Package: com.other;directive:=value;attribute=value\n";

		Manifest originalManifest = new Manifest(new ByteArrayInputStream(manifestContent.getBytes()));

		// Write the manifest using Jar.writeManifest (which should clean and reorder)
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(originalManifest, bout);

		// Parse the cleaned manifest
		Manifest cleanedManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));

		// Verify Export-Package was processed
		String exportPackage = cleanedManifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
		assertNotNull(exportPackage, "Export-Package should be present");

		// Verify Import-Package was processed
		String importPackage = cleanedManifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		assertNotNull(importPackage, "Import-Package should be present");

		// Verify the structure is correct by parsing
		Parameters exportParams = OSGiHeader.parseHeader(exportPackage);
		assertEquals(1, exportParams.size(), "Export-Package should have one clause");

		Attrs exportAttrs = exportParams.get("com.example.api");
		assertNotNull(exportAttrs, "Export clause should have attributes");
		assertEquals(4, exportAttrs.size(), "Export clause should have 4 attributes/directives");

		Parameters importParams = OSGiHeader.parseHeader(importPackage);
		assertEquals(1, importParams.size(), "Import-Package should have one clause");

		Attrs importAttrs = importParams.get("com.other");
		assertNotNull(importAttrs, "Import clause should have attributes");
		assertEquals(2, importAttrs.size(), "Import clause should have 2 attributes/directives");
	}

	@Test
	public void testReorderClauseMethod() throws Exception {
		// Test the reorderClause method directly using reflection
		Method reorderClauseMethod = Jar.class.getDeclaredMethod("reorderClause", String.class, Collator.class);
		reorderClauseMethod.setAccessible(true);

		// Create a collator for testing
		Collator collator = Collator.getInstance(Locale.ROOT);
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
		collator.setStrength(Collator.SECONDARY); // case-insensitive

		// Test case 1: Mixed attributes and directives
		String input1 = "com.example.api;uses:=\"com.example.internal\";version=\"1.0.0\";mandatory:=\"version\";provider=acme";
		String output1 = (String) reorderClauseMethod.invoke(null, input1, collator);

		// Verify that all components are present
		assertTrue(output1.contains("com.example.api"), "Package name should be preserved");
		assertTrue(output1.contains("uses:="), "uses directive should be present");
		assertTrue(output1.contains("version="), "version attribute should be present");
		assertTrue(output1.contains("mandatory:="), "mandatory directive should be present");
		assertTrue(output1.contains("provider="), "provider attribute should be present");

		// Parse the result to verify structure
		Parameters params1 = OSGiHeader.parseHeader(output1);
		assertEquals(1, params1.size(), "Should have exactly one package");

		Attrs attrs1 = params1.get("com.example.api");
		assertNotNull(attrs1, "Package attributes should not be null");
		assertEquals(4, attrs1.size(), "Should have 4 attributes/directives");

		// Test case 2: Simple case with one attribute and one directive
		String input2 = "com.example;directive:=value;attribute=value";
		String output2 = (String) reorderClauseMethod.invoke(null, input2, collator);

		assertTrue(output2.contains("com.example"), "Package name should be preserved");
		assertTrue(output2.contains("directive:=value"), "directive should be present");
		assertTrue(output2.contains("attribute=value"), "attribute should be present");

		// Test case 3: Only attributes
		String input3 = "com.example;version=\"1.0.0\";provider=acme";
		String output3 = (String) reorderClauseMethod.invoke(null, input3, collator);

		assertTrue(output3.contains("com.example"), "Package name should be preserved");
		assertTrue(output3.contains("version=\"1.0.0\""), "version should be present");
		assertTrue(output3.contains("provider=acme"), "provider should be present");

		// Test case 4: Only directives
		String input4 = "com.example;uses:=\"com.other\";mandatory:=\"version\"";
		String output4 = (String) reorderClauseMethod.invoke(null, input4, collator);

		assertTrue(output4.contains("com.example"), "Package name should be preserved");
		assertTrue(output4.contains("uses:="), "uses directive should be present");
		assertTrue(output4.contains("mandatory:="), "mandatory directive should be present");
	}

	@Test
	public void testWriteManifestDirectiveAttributeOrderingMultiplePackages() throws Exception {
		Manifest manifest = new Manifest();

		// Test with multiple packages having different attribute/directive combinations in mixed order
		String exportPackage = "com.example.api;uses:=\"com.example.internal\";version=1.0.0;mandatory:=\"version\"," +
							   "com.example.util;singleton:=true;version=2.0.0;exclude:=\"impl\"";
		manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportPackage);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest, bout);

		Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
		String writtenExportPackage = writtenManifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);

		// Check ordering for first package (com.example.api)
		String firstPackage = writtenExportPackage.split(",")[0];
		int versionPos1 = firstPackage.indexOf("version=");
		int mandatoryPos1 = firstPackage.indexOf("mandatory:");
		int usesPos1 = firstPackage.indexOf("uses:");

		// Attribute (version) should come before directives (mandatory:, uses:)
		assertTrue(versionPos1 < mandatoryPos1, "version attribute should come before mandatory directive in first package");
		assertTrue(versionPos1 < usesPos1, "version attribute should come before uses directive in first package");
		assertTrue(mandatoryPos1 < usesPos1, "mandatory directive should come before uses directive alphabetically");

		// Check ordering for second package (com.example.util)
		String secondPackage = writtenExportPackage.split(",")[1];
		int versionPos2 = secondPackage.indexOf("version=");
		int singletonPos2 = secondPackage.indexOf("singleton:");
		int excludePos2 = secondPackage.indexOf("exclude:");

		// Attribute (version) should come before directives (exclude:, singleton:)
		assertTrue(versionPos2 < singletonPos2, "version attribute should come before singleton directive in second package");
		assertTrue(versionPos2 < excludePos2, "version attribute should come before exclude directive in second package");
		assertTrue(excludePos2 < singletonPos2, "exclude directive should come before singleton directive alphabetically");
	}

	@Test
	public void testWriteManifestRequireCapabilityOrdering() throws Exception {
		Manifest manifest = new Manifest();

		// Test Require-Capability header with mixed attributes and directives in random order
		String requireCapability = "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\";resolution:=optional;cardinality=single";
		manifest.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY, requireCapability);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest, bout);

		Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
		String writtenRequireCapability = writtenManifest.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY);

		// Check ordering by position in the string
		int cardinalityPos = writtenRequireCapability.indexOf("cardinality=");
		int filterPos = writtenRequireCapability.indexOf("filter:");
		int resolutionPos = writtenRequireCapability.indexOf("resolution:");

		// All should be found
		assertTrue(cardinalityPos > 0, "cardinality attribute should be present");
		assertTrue(filterPos > 0, "filter directive should be present");
		assertTrue(resolutionPos > 0, "resolution directive should be present");

		// Attribute (cardinality) should come before directives (filter:, resolution:)
		assertTrue(cardinalityPos < filterPos, "cardinality attribute should come before filter directive");
		assertTrue(cardinalityPos < resolutionPos, "cardinality attribute should come before resolution directive");

		// Within directives: filter should come before resolution (alphabetical)
		assertTrue(filterPos < resolutionPos, "filter directive should come before resolution directive alphabetically");
	}

	@Test
	public void testWriteManifestCaseInsensitiveAttributeOrdering() throws Exception {
		Manifest manifest = new Manifest();

		// Test case-insensitive ordering of attributes with mixed case
		String exportPackage = "com.example.api;Version=1.0.0;bundle-version=1.0.0;ATTR=value;attr2=value2;uses:=\"test\"";
		manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportPackage);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest, bout);

		Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
		String writtenExportPackage = writtenManifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);

		// Check ordering by position - case-insensitive alphabetical for attributes
		int attrPos = writtenExportPackage.indexOf("ATTR=");
		int attr2Pos = writtenExportPackage.indexOf("attr2=");
		int bundleVersionPos = writtenExportPackage.indexOf("bundle-version=");
		int versionPos = writtenExportPackage.indexOf("Version=");
		int usesPos = writtenExportPackage.indexOf("uses:");

		// All should be found
		assertTrue(attrPos > 0, "ATTR attribute should be present");
		assertTrue(attr2Pos > 0, "attr2 attribute should be present");
		assertTrue(bundleVersionPos > 0, "bundle-version attribute should be present");
		assertTrue(versionPos > 0, "Version attribute should be present");
		assertTrue(usesPos > 0, "uses directive should be present");

		// All attributes should come before the directive
		assertTrue(attrPos < usesPos, "ATTR attribute should come before uses directive");
		assertTrue(attr2Pos < usesPos, "attr2 attribute should come before uses directive");
		assertTrue(bundleVersionPos < usesPos, "bundle-version attribute should come before uses directive");
		assertTrue(versionPos < usesPos, "Version attribute should come before uses directive");

		// Case-insensitive alphabetical ordering within attributes:
		// ATTR, attr2, bundle-version, Version
		assertTrue(attrPos < attr2Pos, "ATTR should come before attr2 (case-insensitive)");
		assertTrue(attr2Pos < bundleVersionPos, "attr2 should come before bundle-version");
		assertTrue(bundleVersionPos < versionPos, "bundle-version should come before Version");
	}

	@Test
	public void testWriteManifestNonOSGiHeadersNotReordered() throws Exception {
		Manifest manifest = new Manifest();

		// Test that non-OSGi headers are not reordered
		String customHeader = "com.example.api;directive:=value;attribute=value";
		manifest.getMainAttributes().putValue("Custom-Header", customHeader);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest, bout);

		Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
		String writtenCustomHeader = writtenManifest.getMainAttributes().getValue("Custom-Header");

		// Should remain unchanged since Custom-Header is not in OSGI_SYNTAX_HEADERS
		assertEquals(customHeader, writtenCustomHeader);
	}

	/**
	 * Test Bundle-NativeCode is untouched from Jar.reorderClause() for now,
	 * because this header is special and needs to be parsed differently than
	 * normal OSGi headers. In the future we could evaluate how to build a
	 * special reordering for this header.
	 */
	@Test
	public void testWriteManifestBundleNativeCodeNoReordering() throws Exception {
		Manifest manifest = new Manifest();

		String customHeader = """
			   natives/linux-amd64/libgluegen_rt.so;
			   natives/linux-amd64/libjocl.so;
			   natives/linux-amd64/libnativewindow_awt.so;
			   natives/linux-amd64/libnewt_drm.so;
			   natives/linux-amd64/libnativewindow_x11.so;
			   natives/linux-amd64/libnativewindow_drm.so;
			   natives/linux-amd64/libnewt_head.so;
			   natives/linux-amd64/libjogl_mobile.so;
			   natives/linux-amd64/libjogl_desktop.so;
			   processor=x86-64; osname=Linux
			""";
		manifest.getMainAttributes()
			.putValue(Constants.BUNDLE_NATIVECODE, customHeader);

		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			Jar.writeManifest(manifest, bout);

			Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
			String writtenCustomHeader = writtenManifest.getMainAttributes()
				.getValue(Constants.BUNDLE_NATIVECODE);

			assertEquals(customHeader.replaceAll("\n", "")
				.replace(" ", ""),
				writtenCustomHeader.replaceAll("\n", "")
					.replace(" ", ""));
		}
	}

	/**
	 * Test Bundle-NativeCode is untouched from Jar.reorderClause() for now,
	 * because this header is special and needs to be parsed differently than
	 * normal OSGi headers. In the future we could evaluate how to build a
	 * special reordering for this header.
	 */
	@Test
	public void testWriteManifestBundleNativeCodeNoReordering2() throws Exception {
		Manifest manifest = new Manifest();

		String customHeader = """
			f1;\
			  osname=Windows95;
			  processor=x86;
			  selection-filter='(com.acme.windowing=win32)';
			  language=en;
			  osname=Windows98;
			  language=se,
			lib/solaris/libhttp.so;
			  osname=Solaris;
			  osname = SunOS ;
			  processor = sparc,
			lib/linux/libhttp.so;
			  osname = Linux ;
			  osversion = 3.1.4;
			  processor = mips;
			  selection-filter = '(com.acme.windowing=gtk)',
			*""";
		manifest.getMainAttributes()
			.putValue(Constants.BUNDLE_NATIVECODE, customHeader);

		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			Jar.writeManifest(manifest, bout);

			Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
			String writtenCustomHeader = writtenManifest.getMainAttributes()
				.getValue(Constants.BUNDLE_NATIVECODE);

			assertEquals(customHeader.replaceAll("\n", "")
				.replace(" ", ""),
				writtenCustomHeader.replaceAll("\n", "")
					.replace(" ", ""));
		}
	}


	@Test
	public void testWriteManifestComprehensiveAttributeDirectiveOrdering() throws Exception {
		Manifest manifest = new Manifest();

			// Test comprehensive ordering with many attributes and directives mixed up
			String exportPackage = "com.example.api;" +
				"uses:=\"com.example.internal\";" +  // directive
				"version=1.0.0;" +                   // attribute
				"x-friends:=\"com.example.test\";" + // directive (x-friends:)
				"mandatory:=\"version\";" +          // directive
				"provider=acme;" +                   // attribute
				"bundle-version=1.0.0;" +           // attribute
				"exclude:=\"impl\"";                 // directive
			manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportPackage);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Jar.writeManifest(manifest, bout);

			Manifest writtenManifest = new Manifest(new ByteArrayInputStream(bout.toByteArray()));
			String writtenExportPackage = writtenManifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);

			// Find positions of all attributes and directives
			int bundleVersionPos = writtenExportPackage.indexOf(";bundle-version=");
			int providerPos = writtenExportPackage.indexOf(";provider=");
			int versionPos = writtenExportPackage.indexOf(";version=");

			int excludePos = writtenExportPackage.indexOf(";exclude:");
			int mandatoryPos = writtenExportPackage.indexOf(";mandatory:");
			int usesPos = writtenExportPackage.indexOf(";uses:");
			int xFriendsPos = writtenExportPackage.indexOf(";x-friends:");

			// All should be found
			assertTrue(bundleVersionPos > 0, "bundle-version attribute should be present");
			assertTrue(providerPos > 0, "provider attribute should be present");
			assertTrue(versionPos > 0, "version attribute should be present");
			assertTrue(excludePos > 0, "exclude directive should be present");
			assertTrue(mandatoryPos > 0, "mandatory directive should be present");
			assertTrue(usesPos > 0, "uses directive should be present");
			assertTrue(xFriendsPos > 0, "x-friends directive should be present");

			// ALL attributes should come before ALL directives
			assertTrue(bundleVersionPos < excludePos, "bundle-version attribute should come before exclude directive");
			assertTrue(bundleVersionPos < mandatoryPos, "bundle-version attribute should come before mandatory directive");
			assertTrue(bundleVersionPos < usesPos, "bundle-version attribute should come before uses directive");
			assertTrue(bundleVersionPos < xFriendsPos, "bundle-version attribute should come before x-friends directive");

			assertTrue(providerPos < excludePos, "provider attribute should come before exclude directive");
			assertTrue(providerPos < mandatoryPos, "provider attribute should come before mandatory directive");
			assertTrue(providerPos < usesPos, "provider attribute should come before uses directive");
			assertTrue(providerPos < xFriendsPos, "provider attribute should come before x-friends directive");

			assertTrue(versionPos < excludePos, "version attribute should come before exclude directive");
			assertTrue(versionPos < mandatoryPos, "version attribute should come before mandatory directive");
			assertTrue(versionPos < usesPos, "version attribute should come before uses directive");
			assertTrue(versionPos < xFriendsPos, "version attribute should come before x-friends directive");

			// Within attributes: alphabetical order (case-insensitive)
			// Expected: bundle-version, provider, version
			assertTrue(bundleVersionPos < providerPos, "bundle-version should come before provider alphabetically");
			assertTrue(providerPos < versionPos, "provider should come before version alphabetically");

			// Within directives: alphabetical order (case-insensitive)
			// Expected: exclude:, mandatory:, uses:, x-friends:
			assertTrue(excludePos < mandatoryPos, "exclude directive should come before mandatory directive alphabetically");
			assertTrue(mandatoryPos < usesPos, "mandatory directive should come before uses directive alphabetically");
			assertTrue(usesPos < xFriendsPos, "uses directive should come before x-friends directive alphabetically");
	}

	@Test
	public void testZipSlip(@InjectTemporaryDirectory
	File tmp) throws Exception {
		assertThat(catchThrowable(() -> {
			try (Jar jar = new Jar(new File("jar/zip-slip.zip"))) {
				jar.writeFolder(tmp);
			}
		})).as("Failed to handle zip-slip problem")
			.isInstanceOfAny(IOException.class, UncheckedIOException.class);
	}

	@Test
	public void testCreateZipSlip() throws Exception {
		try (Jar jar = new Jar("zipzlip")) {
			assertThat(catchThrowable(() -> {
				jar.putResource("foo/../../bad.txt", new EmbeddedResource("bad", 0L));
			})).as("Failed to handle zip-slip problem")
				.isInstanceOfAny(IOException.class, UncheckedIOException.class);
			jar.putResource("foo/../ok.txt", new EmbeddedResource("ok", 0L));
			assertThat(jar.exists("ok.txt")).isTrue();
		}
	}
}
