package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
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
	 * Simplest reproducer for bndtools/bnd#7452 (regression since bnd 7.2.0).
	 * <p>
	 * Setup: {@code inner.jar} is made by a {@code -make} recipe
	 * ({@code inner.bnd}) and is put on the {@code Bundle-ClassPath} of the
	 * main bundle, like the OSGi framework TCK does with its test bundles.
	 * <p>
	 * What goes wrong on bnd 7.2.0+:
	 * <ol>
	 * <li>{@code -includeresource: inner.jar} makes the jar and adds it to the
	 * main jar as a {@link JarResource}, which <em>wraps the made
	 * {@link Jar} itself</em> (no copy).</li>
	 * <li>While analyzing the main bundle,
	 * {@code CDIAnnotations.analyzeJar} looks into every
	 * {@code Bundle-ClassPath} entry for {@code META-INF/beans.xml}. It does
	 * that with {@code try (Jar jar = Jar.fromResource(path, resource))}. For
	 * a {@link JarResource}, {@code fromResource} returns the wrapped jar, so
	 * the try-with-resources <b>closes the made inner jar</b> (introduced with
	 * "speedup CDIAnnotations", d4cdefdd2).</li>
	 * <li>The build itself reports no problem, but writing the main jar (what
	 * the bnd-maven-plugin does after the build) fails with
	 * {@code Already closed inner}.</li>
	 * </ol>
	 */
	@Test
	public void testMadeJarOnBundleClassPath(@InjectTemporaryDirectory
	File tmp) throws Exception {
		System.err.println("\n=== #7452: made jar on Bundle-ClassPath ===");
		IO.store("Bundle-SymbolicName: inner\n-includeresource: inner.txt;literal=\"inner\"\n", new File(tmp, "inner.bnd"));

		try (Builder b = new Builder()) {
			b.setBase(tmp);
			b.setProperty("-make", "(*).jar;type=bnd;recipe=$1.bnd");
			b.setProperty("Bundle-ClassPath", "inner.jar");
			b.setProperty("-includeresource", "inner.jar");

			Jar jar = b.build();
			report(b);
			System.err.println("1. build() done, errors=" + b.getErrors() + " (none expected)");
			assertTrue(b.getErrors()
				.isEmpty(), "the build itself must not report errors");

			Resource inner = jar.getResource("inner.jar");
			assertTrue(inner instanceof JarResource, "inner.jar is included as JarResource wrapping the made Jar");
			Jar madeJar = ((JarResource) inner).getJar();
			System.err.println("2. made inner Jar '" + madeJar.getName() + "' is wrapped by the JarResource");

			// This is what the bnd-maven-plugin does: write the finished jar.
			try {
				jar.write(new ByteArrayOutputStream());
				System.err.println("3. jar.write() OK");
			} catch (Exception e) {
				System.err.println("3. jar.write() FAILED, root cause: " + rootCause(e));
				System.err.println("   -> the made inner jar was closed during the build, by "
					+ "CDIAnnotations.analyzeJar (try-with-resources on Jar.fromResource(..))");
				throw e;
			}
		}
	}

	private static Throwable rootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		return t;
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
