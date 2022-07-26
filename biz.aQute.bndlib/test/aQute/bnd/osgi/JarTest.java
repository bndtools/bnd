package aQute.bnd.osgi;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JarTest {

	private static final String	MODULE_INFO_CLASS	= "module-info.class";
	private static final String	TEST_CLASS_PATH		= "a/test/package/Test.class";

	@TempDir
	File						tempDir;

	@Test
	public void testMultiReleaseJar() throws Exception {
		File jarfile = new File(tempDir, "packed.jar");
		try (Jar jar = new Jar("testme")) {
			jar.ensureManifest();
			Resource moduleInfo = resource();
			Resource testClass = resource();
			jar.putResource(TEST_CLASS_PATH, testClass);
			jar.setRelease(9);
			jar.putResource(MODULE_INFO_CLASS, moduleInfo);
			assertEquals(moduleInfo, jar.getResource(MODULE_INFO_CLASS));
			assertEquals(testClass, jar.getResource(TEST_CLASS_PATH));
			jar.setRelease(0);
			assertNull(jar.getResource(MODULE_INFO_CLASS));
			jar.writeFolder(tempDir);
			assertTrue(new File(tempDir, TEST_CLASS_PATH).isFile());
			assertFalse(new File(tempDir, MODULE_INFO_CLASS).isFile());
			assertTrue(new File(tempDir, "META-INF/versions/9/" + MODULE_INFO_CLASS).isFile());
			File file = new File(tempDir, JarFile.MANIFEST_NAME);
			assertTrue(file.isFile());
			try (FileInputStream is = new FileInputStream(file)) {
				Manifest manifest = new Manifest(is);
				assertEquals("true", manifest.getMainAttributes()
					.getValue("Multi-Release"));
			}
			jar.write(jarfile);
		}
		try (JarFile jar = new JarFile(jarfile)) {
			assertEquals("true", jar.getManifest()
				.getMainAttributes()
				.getValue("Multi-Release"));
			Set<String> collect = Collections.list(jar.entries())
				.stream()
				.map(JarEntry::getName)
				.collect(Collectors.toSet());
			assertFalse(collect.contains(MODULE_INFO_CLASS));
			assertTrue(collect.contains(TEST_CLASS_PATH));
			assertTrue(collect.contains("META-INF/versions/9/" + MODULE_INFO_CLASS));
		}
	}

	@Test
	public void testGson() throws Exception {
		// just for demontration purpose...
		File f = new File("/tmp/gson-2.9.0.jar");
		if (f.isFile()) {
			try (Jar jar = new Jar(f)) {
				assertNull(jar.getModuleName());
				jar.setRelease(9);
				assertEquals("com.google.gson", jar.getModuleName());
			}
		}
	}

	private Resource resource() {
		return new aQute.bnd.osgi.EmbeddedResource(new byte[0], System.currentTimeMillis());
	}

}
