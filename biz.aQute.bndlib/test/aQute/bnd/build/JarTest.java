package aQute.bnd.build;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.builder.ModuleInfoBuilder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.ByteBufferDataOutput;

public class JarTest {

	private static final String	MODULE_INFO_PATH			= "META-INF/versions/9/" + Constants.MODULE_INFO_CLASS;

	private static final String	MAIN_MANIFEST_PATH			= JarFile.MANIFEST_NAME;

	private static final String	SUPPLEMENTAL_MANIFEST_PATH	= "META-INF/versions/9/OSGI-INF/MANIFEST.MF";

	private static final String	TEST_CLASS_PATH				= "a/test/package/Test.class";

	private static final String	VERSIONED_TEST_CLASS_PATH	= "META-INF/versions/9/" + TEST_CLASS_PATH;

	@TempDir
	File						tempDir;

	@Test
	public void testMultiReleaseJarResources() throws Exception {
		File jarfile = new File(tempDir, "packed.jar");
		try (Jar jar = new Jar("testme")) {
			jar.setMultiRelease(true);
			Resource java8Class = resource();
			Resource java9Class = resource();
			jar.putResource(TEST_CLASS_PATH, java8Class);
			jar.putResource(VERSIONED_TEST_CLASS_PATH, java9Class);
			// without release, content must be returned as-is
			assertTrue(jar.isMultiRelease());
			assertEquals(java8Class, jar.getResource(TEST_CLASS_PATH));
			assertEquals(java9Class, jar.getResource(VERSIONED_TEST_CLASS_PATH));
			// with release 9 set, we now should see the java 9 content
			assertEquals(java9Class, jar.getVersionedResource(TEST_CLASS_PATH, 9)
				.orElse(null));
			// with a lower release we should get the java 8 content
			assertEquals(java8Class, jar.getVersionedResource(TEST_CLASS_PATH, 8)
				.orElse(null));
			assertEquals(java8Class, jar.getVersionedResource(TEST_CLASS_PATH, -1)
				.orElse(null));
			assertEquals(java8Class, jar.getVersionedResource(TEST_CLASS_PATH, 4)
				.orElse(null));
			// with a higher release we should still get the java 9 content
			assertEquals(java9Class, jar.getVersionedResource(TEST_CLASS_PATH, 10)
				.orElse(null));
			assertEquals(java9Class, jar.getVersionedResource(TEST_CLASS_PATH, 1000)
				.orElse(null));
			// if we write the jar out, all content should be present
			jar.writeFolder(tempDir);
			File defaultFile = new File(tempDir, TEST_CLASS_PATH);
			assertTrue(defaultFile.isFile(), defaultFile.getAbsolutePath() + " is missing");
			File versionedFile = new File(tempDir, VERSIONED_TEST_CLASS_PATH);
			assertTrue(versionedFile.isFile(), versionedFile.getAbsolutePath() + " is missing");
			jar.write(jarfile);
		}
		try (JarFile jar = new JarFile(jarfile)) {
			Set<String> collect = Collections.list(jar.entries())
				.stream()
				.map(JarEntry::getName)
				.collect(Collectors.toSet());
			assertTrue(collect.contains(TEST_CLASS_PATH));
			assertTrue(collect.contains(VERSIONED_TEST_CLASS_PATH));
			assertTrue(Boolean.parseBoolean(jar.getManifest()
				.getMainAttributes()
				.getValue("Multi-Release")));
		}
	}

	@Test
	public void testMultiReleaseJaManifest() throws Exception {
		try (Jar jar = new Jar("testme")) {
			String defaultImport = "main.pkg.import";
			String suplementalImport = "main.pkg.import,additional.package";
			String ignoredValue = "This Must Be Ignored";
			jar.putResource(MAIN_MANIFEST_PATH,
				manifest("Multi-Release", "true", Constants.IMPORT_PACKAGE, defaultImport));
			jar.putResource(SUPPLEMENTAL_MANIFEST_PATH, manifest(Constants.IMPORT_PACKAGE, suplementalImport,
				Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE, ignoredValue));
			assertTrue(jar.isMultiRelease());
			Manifest mainManifest = Objects.requireNonNull(jar.getManifest());
			Manifest versionedManifest = jar.getManifest(9)
				.get();
			assertNotEquals(mainManifest, versionedManifest);
			assertEquals(defaultImport, mainManifest.getMainAttributes()
				.getValue(Constants.IMPORT_PACKAGE));
			assertEquals(suplementalImport, versionedManifest.getMainAttributes()
				.getValue(Constants.IMPORT_PACKAGE));
			assertNull(mainManifest.getMainAttributes()
				.getValue(Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE));
			assertNull(versionedManifest.getMainAttributes()
				.getValue(Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE));

		}
	}

	@Test
	public void testMultiReleaseJaModule() throws Exception {
		try (Jar jar = new Jar("testme")) {
			jar.setMultiRelease(true);
			String moduleName = "my.name";
			String moduleVersion = "1.0.0";
			jar.putResource(MODULE_INFO_PATH, module(moduleName, moduleVersion));
			assertNull(jar.getModuleName());
			assertEquals(moduleName, jar.getModuleName(9));
			assertNull(jar.getModuleVersion());
			assertEquals(moduleVersion, jar.getModuleVersion(9));
		}
	}

	/**
	 * This test assumes the following layout:
	 *
	 * <pre>
	 * /Foo.class
	 * /Bar.class
	 * /META-INF/versions/9/Foo.class
	 * /META-INF/versions/11/Bar.class
	 * /META-INF/versions/17/Foo.class
	 * </pre>
	 *
	 * Calling
	 * <ul>
	 * <li>getVersionedResource(Foo.class, 11) - I would expect to get returned
	 * the resource at /META-INF/versions/9/Foo.class</li>
	 * <li>getVersionedResource(Bar.class, 9) - I would expect to get returned
	 * the resource at /Bar.class</li>
	 * <li>getVersionedResource(Foo.class, 17) - I would expect to get returned
	 * the resource at /META-INF/versions/17/Foo.class</li>
	 * </ul>
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultiReleaseJarMultipleResources() throws Exception {
		try (Jar jar = new Jar("testme")) {
			jar.setMultiRelease(true);
			Resource Foo = resource();
			Resource Bar = resource();
			Resource Foo9 = resource();
			Resource Bar11 = resource();
			Resource Foo17 = resource();
			String fooPath = "Foo.class";
			String barPath = "Bar.class";
			jar.putResource(fooPath, Foo);
			jar.putResource(barPath, Bar);
			jar.putResource("META-INF/versions/9/" + fooPath, Foo9);
			jar.putResource("META-INF/versions/11/" + barPath, Bar11);
			jar.putResource("META-INF/versions/17/" + fooPath, Foo17);
			assertEquals(Foo9, jar.getVersionedResource(fooPath, 11)
				.get());
			assertEquals(Bar, jar.getVersionedResource(barPath, 9)
				.get());
			assertEquals(Foo17, jar.getVersionedResource(fooPath, 17)
				.get());
		}
	}

	private static Resource module(String moduleName, String moduleVersion) throws IOException {
		ModuleInfoBuilder builder = new ModuleInfoBuilder().module_name(moduleName)
			.module_version(moduleVersion)
			.module_flags(0);
		ClassFile build = builder.build();
		ByteBufferDataOutput bbout = new ByteBufferDataOutput();
		builder.build()
			.write(bbout);
		EmbeddedResource resource = new EmbeddedResource(bbout.toByteBuffer(), System.currentTimeMillis());
		return resource;
	}

	private static Resource resource() {
		return new aQute.bnd.osgi.EmbeddedResource(new byte[0], System.currentTimeMillis());
	}

	private static Resource manifest(String k1, String v1, String k2, String v2) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes()
			.putValue(k1, v1);
		manifest.getMainAttributes()
			.putValue(k2, v2);
		return new ManifestResource(manifest);
	}

}
