package test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import aQute.bnd.make.MakeBnd;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

/**
 * Tests the make functionality.
 */
@SuppressWarnings("resource")
public class MakeTest {

	/**
	 * Test a make plugin
	 */

	@Test
	public void testMakePlugin() throws Exception {
		try (Builder bmaker = new Builder()) {
			bmaker.setProperty("Export-Package", "*");
			bmaker.setProperty("Include-Resource", "jar/asm.jar.md5");
			bmaker.setProperty("-make", "(*).md5;type=md5;file=$1");
			bmaker.setProperty("-plugin", "test.make.MD5");
			bmaker.addClasspath(IO.getFile("jar/osgi.jar"));
			Jar jar = bmaker.build();
			report(bmaker);

			assertNotNull(jar.getResource("asm.jar.md5"));
		}
	}

	/**
	 * Check if we can get a resource through the make copy facility.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCopy() throws Exception {
		try (Builder bmaker = new Builder()) {
			Properties p = new Properties();
			p.setProperty("-resourceonly", "true");
			p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd, (*).jar;type=copy;from=jar/$1.jar");
			p.setProperty("Include-Resource", "asm.jar,xyz=asm.jar");
			bmaker.setProperties(p);
			Jar jar = bmaker.build();
			report(bmaker);

			assertNotNull(jar.getResource("asm.jar"));
			assertNotNull(jar.getResource("xyz"));
		}
	}

	/**
	 * Check if we can create a JAR recursively
	 *
	 * @throws Exception
	 */
	@Test
	public void testJarInJarInJar() throws Exception {
		try (Builder bmaker = new Builder()) {
			Properties p = new Properties();
			p.setProperty("-resourceonly", "true");
			p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
			p.setProperty("Include-Resource", "makesondemand.jar");
			bmaker.setProperties(p);
			bmaker.setClasspath(new String[] {
				"bin_test"
			});
			Jar jar = bmaker.build();
			report(bmaker);

			JarResource resource = (JarResource) jar.getResource("makesondemand.jar");
			assertNotNull(resource);

			jar = resource.getJar();
			resource = (JarResource) jar.getResource("ondemand.jar");
			assertNotNull(resource);
		}
	}

	/**
	 * Check that inner jar does not include filtered headers
	 *
	 * @throws Exception
	 */
	@Test
	public void testFilteredHeader() throws Exception {
		try (Builder bmaker = new Builder()) {
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.activator");
			p.setProperty("Provide-Capability", "foo");
			p.setProperty("Bundle-Activator", "test.activator.Activator");
			p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
			p.setProperty("-includeresource", "noactivator.jar");
			bmaker.setProperties(p);
			bmaker.setClasspath(new String[] {
				"bin_test"
			});
			Jar jar = bmaker.build();
			report(bmaker);

			Attributes m = jar.getManifest()
				.getMainAttributes();
			assertEquals("test.activator.Activator", m.getValue("Bundle-Activator"));
			assertEquals("foo", m.getValue("Provide-Capability"));
			JarResource resource = (JarResource) jar.getResource("noactivator.jar");
			assertNotNull(resource);
			jar = resource.getJar();
			m = jar.getManifest()
				.getMainAttributes();
			assertNull(m.getValue("Bundle-Activator"));
			assertNull(m.getValue("Provide-Capability"));
		}
	}

	@Test
	public void testNestedMakeJarsDoNotCloseInnerJar() throws Exception {
		Path base = Files.createTempDirectory("nested-make");
		try {
			Path recipeDir = Files.createDirectories(base.resolve("bnd"));
			Files.writeString(recipeDir.resolve("inner.bnd"),
				"Bundle-SymbolicName: example.inner\n-includeresource: inner.txt;literal=\"inner\"\n");
			Files.writeString(recipeDir.resolve("outer1.bnd"),
				"Bundle-SymbolicName: example.outer1\n-manifest-name: custom.mf\nBundle-ClassPath: inner.jar\n-includeresource: inner.jar\n");

			try (Builder builder = new Builder()) {
				builder.setBase(base.toFile());
				Resource outer = new MakeBnd().make(builder, "outer1.jar", Map.of("type", "bnd", "recipe", "bnd/outer1.bnd"));
				assertNotNull(outer);
				assertTrue(outer instanceof JarResource);
				assertEquals("custom.mf", ((JarResource) outer).getJar().getManifestName());

				try (Jar container = new Jar("container")) {
					container.putResource("outer1.jar", outer);
					builder.close();

					assertDoesNotThrow(() -> container.write(new ByteArrayOutputStream()));
				}
			}
		} finally {
			IO.delete(base.toFile());
		}
	}

	@Test
	public void testNestedMakeJarLifecycleThroughBuilder() throws Exception {
		Path base = Files.createTempDirectory("nested-make-builder");
		try {
			Files.writeString(base.resolve("main.bnd"),
				"-resourceonly: true\n" +
					"Include-Resource: outer1.jar, outer2.jar\n");
			Files.writeString(base.resolve("outer1.bnd"),
				"-resourceonly: true\n" +
					"-make: (*).jar;type=bnd;recipe=$1.bnd\n" +
					"Include-Resource: outer1.txt, inner.jar\n");
			Files.writeString(base.resolve("outer2.bnd"),
				"-resourceonly: true\n" +
					"-make: (*).jar;type=bnd;recipe=$1.bnd\n" +
					"Include-Resource: outer2.txt, inner.jar\n");
			Files.writeString(base.resolve("inner.bnd"), "-resourceonly: true\nInclude-Resource: inner.txt\n");
			Files.writeString(base.resolve("outer1.txt"), "outer1\n");
			Files.writeString(base.resolve("outer2.txt"), "outer2\n");
			Files.writeString(base.resolve("inner.txt"), "inner\n");

			try (Builder root = new Builder()) {
				Properties properties = new Properties();
				properties.setProperty("-resourceonly", "true");
				properties.setProperty("-make", "(*).jar;type=bnd;recipe=$1.bnd");
				properties.setProperty("Include-Resource", "main.jar");
				root.setBase(base.toFile());
				root.setProperties(properties);

				Jar jar = root.build();
				report(root);
				assertNotNull(jar.getResource("main.jar"));

				Path output = base.resolve("nested-main.jar");
				assertDoesNotThrow(() -> jar.write(output.toFile()));
			}
		} finally {
			IO.delete(base.toFile());
		}
	}

	@Test
	public void testMadeJarPreservesManifestModeAndReproducibleTimestamp() throws Exception {
		Path base = Files.createTempDirectory("made-jar-metadata");
		try {
			Files.writeString(base.resolve("recipe.bnd"),
				"-resourceonly: true\n" +
					"-nomanifest: true\n" +
					"-reproducible: 1234567890\n" +
					"Include-Resource: data.txt\n");
			Files.writeString(base.resolve("data.txt"), "data\n");

			try (Builder builder = new Builder()) {
				builder.setBase(base.toFile());
				Resource resource = new MakeBnd().make(builder, "made.jar",
					Map.of("type", "bnd", "recipe", "recipe.bnd"));
				assertTrue(resource instanceof JarResource);
				assertEquals(1234567890000L, ((JarResource) resource).getJar().getReproducibleTimestamp());

				ByteArrayOutputStream output = new ByteArrayOutputStream();
				resource.write(output);
				try (ZipInputStream input = new ZipInputStream(new java.io.ByteArrayInputStream(output.toByteArray()))) {
					ZipEntry entry = input.getNextEntry();
					assertNotNull(entry);
					assertEquals("data.txt", entry.getName());
					assertNotEquals(318211200000L, entry.getTime());
					assertNull(input.getNextEntry());
				}
			}
		} finally {
			IO.delete(base.toFile());
		}
	}

	@Test
	public void testChildSpecificClasspathIsClosed() throws Exception {
		Path base = Files.createTempDirectory("made-jar-classpath");
		try {
			Path childJar = base.resolve("child.jar");
			try (Jar child = new Jar("child")) {
				child.putResource("child.txt", new EmbeddedResource("child", 0));
				child.write(childJar.toFile());
			}
			Files.writeString(base.resolve("recipe.bnd"),
				"-classpath: child.jar\n" +
					"-resourceonly: true\n" +
					"Include-Resource: data.txt\n");
			Files.writeString(base.resolve("data.txt"), "data\n");

			try (Builder builder = new Builder()) {
				builder.setBase(base.toFile());
				assertNotNull(new MakeBnd().make(builder, "made.jar",
					Map.of("type", "bnd", "recipe", "recipe.bnd")));
			}
			Files.delete(childJar);
		} finally {
			IO.delete(base.toFile());
		}
	}

	/**
	 * Check if we can create a jar on demand through the make facility with a
	 * new name.
	 *
	 * @throws Exception
	 */
	@Test
	public void testComplexOnDemand() throws Exception {
		try (Builder bmaker = new Builder()) {
			Properties p = new Properties();
			p.setProperty("-resourceonly", "true");
			p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
			p.setProperty("Include-Resource", "www/xyz.jar=ondemand.jar");
			bmaker.setProperties(p);
			bmaker.setClasspath(new String[] {
				"bin_test"
			});
			Jar jar = bmaker.build();
			report(bmaker);

			Resource resource = jar.getResource("www/xyz.jar");
			assertNotNull(resource);
			assertTrue(resource instanceof JarResource);
		}
	}

	static void report(Processor processor) {
		System.err.println();
		for (String element : processor.getErrors())
			System.err.println(element);
		for (String element : processor.getWarnings())
			System.err.println(element);
		assertEquals(0, processor.getErrors()
			.size());
		assertEquals(0, processor.getWarnings()
			.size());
	}

}
