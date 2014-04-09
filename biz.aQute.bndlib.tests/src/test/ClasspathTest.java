package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

@SuppressWarnings("resource")
public class ClasspathTest extends TestCase {

	/**
	 * Test if we can refer to the jars on the classpath by their file name (
	 * ignoring the path)
	 * 
	 * @throws Exception
	 */
	public static void testBundleClasspath() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "bin=bin");
		b.setProperty(Constants.BUNDLE_CLASSPATH, "bin");

		Jar jar = b.build();
		assertNotNull(jar.getResource("bin/test/activator/Activator.class")); // from
		// test.jar
	}

	/**
	 * Test if we can refer to the jars on the classpath by their file name (
	 * ignoring the path)
	 * 
	 * @throws Exception
	 */
	public static void testFindJarOnClasspath() throws Exception {
		Properties p = new Properties();
		p.put(Constants.INCLUDE_RESOURCE, "tb1.jar, @test.jar");

		Builder b = new Builder();
		b.setClasspath(new String[] {
				"src", "src/test/test.jar", "src/test/tb1.jar"
		});
		b.setProperties(p);
		Jar jar = b.build();
		assertNotNull(jar.getResource("aQute/lib/aim/AIM.class")); // from
																	// test.jar
		assertNotNull(jar.getResource("tb1.jar"));
	}

	/**
	 * Test if we can use URLs on the classpath
	 * 
	 * @throws Exception
	 */
	public static void testSimple() throws Exception {
		Properties p = new Properties();
		p.put("-classpath", new File("jar/osgi.jar").toURI().toURL().toString());
		p.put(Constants.EXPORT_PACKAGE, "org.osgi.service.event");
		p.put(Constants.PRIVATE_PACKAGE, "test.refer");

		Builder b = new Builder();
		b.setClasspath(new String[] {
			"bin"
		});
		b.setProperties(p);
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		String importPackage = m.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		assertTrue(importPackage.contains("org.osgi.framework;version=\"[1.3,2)\""));
		assertTrue(importPackage.contains("org.osgi.service.event;version=\"[1.0,2)\""));
	}

}
