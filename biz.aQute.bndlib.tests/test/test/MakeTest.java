package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Properties;
import java.util.jar.Attributes;

import org.junit.jupiter.api.Test;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;

import aQute.bnd.osgi.Builder;
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

	/**
	 * A jar made by a -make recipe that is embedded via Bundle-ClassPath in
	 * another made jar must still be usable when the outer jar is written. The
	 * CDI analysis of the Bundle-ClassPath entries used to close the shared
	 * inner Jar (bndtools/bnd#7452).
	 */
	@Test
	public void testNestedMakeJarOnBundleClassPath(@InjectTemporaryDirectory
	File tmp) throws Exception {
		IO.store("inner", new File(tmp, "inner.txt"));
		IO.store("Bundle-SymbolicName: example.inner\n-includeresource: inner.txt\n", new File(tmp, "inner.bnd"));
		for (String n : new String[] {
			"outer1", "outer2"
		}) {
			IO.store("Bundle-SymbolicName: example." + n + "\nBundle-ClassPath: inner.jar\n"
				+ "-includeresource: inner.jar\n", new File(tmp, n + ".bnd"));
		}
		IO.store("Bundle-SymbolicName: example.main\n-make: (*).jar;type=bnd;recipe=$1.bnd\n"
			+ "-includeresource: outer1.jar, outer2.jar\n", new File(tmp, "main.bnd"));

		try (Builder main = new Builder()) {
			main.setBase(tmp);
			main.setProperties(new File(tmp, "main.bnd"));
			Jar jar = main.build();
			report(main);
			assertTrue(main.isOk());

			File out = new File(tmp, "out.jar");
			jar.write(out);

			try (Jar written = new Jar(out)) {
				for (String n : new String[] {
					"outer1.jar", "outer2.jar"
				}) {
					Resource outer = written.getResource(n);
					assertNotNull(outer);
					try (Jar outerJar = new Jar(n, outer.openInputStream())) {
						assertNotNull(outerJar.getResource("inner.jar"));
					}
				}
			}
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
