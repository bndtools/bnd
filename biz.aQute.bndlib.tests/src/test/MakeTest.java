package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

/**
 * Tests the make functionality.
 * 
 */
@SuppressWarnings("resource")
public class MakeTest extends TestCase {

	/**
	 * Test a make plugin
	 */

	public static void testMakePlugin() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.EXPORT_PACKAGE, "*");
		b.setProperty(Constants.INCLUDE_RESOURCE, "jar/asm.jar.md5");
		b.setProperty("-make", "(*).md5;type=md5;file=$1");
		b.setProperty("-plugin", "test.make.MD5");
		b.addClasspath(new File("jar/osgi.jar"));
		Jar jar = b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		assertNotNull(jar.getResource("asm.jar.md5"));
	}

	/**
	 * Check if we can get a resource through the make copy facility.
	 * 
	 * @throws Exception
	 */
	public static void testCopy() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
		p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd, (*).jar;type=copy;from=jar/$1.jar");
		p.setProperty(Constants.INCLUDE_RESOURCE, "asm.jar,xyz=asm.jar");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertNotNull(jar.getResource("asm.jar"));
		assertNotNull(jar.getResource("xyz"));
		report(bmaker);

	}

	/**
	 * Check if we can create a JAR recursively
	 * 
	 * @throws Exception
	 */
	public static void testJarInJarInJar() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
		p.setProperty("-resourceonly", "true");
		p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
		p.setProperty(Constants.INCLUDE_RESOURCE, "makesondemand.jar");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"bin"
		});
		Jar jar = bmaker.build();
		JarResource resource = (JarResource) jar.getResource("makesondemand.jar");
		assertNotNull(resource);

		jar = resource.getJar();
		resource = (JarResource) jar.getResource("ondemand.jar");
		assertNotNull(resource);

		report(bmaker);
	}

	/**
	 * Check if we can create a jar on demand through the make facility with a
	 * new name.
	 * 
	 * @throws Exception
	 */
	public static void testComplexOnDemand() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
		p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
		p.setProperty(Constants.INCLUDE_RESOURCE, "www/xyz.jar=ondemand.jar");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"bin"
		});
		Jar jar = bmaker.build();
		Resource resource = jar.getResource("www/xyz.jar");
		assertNotNull(resource);
		assertTrue(resource instanceof JarResource);
		report(bmaker);

	}

	static void report(Processor processor) {
		System.err.println();
		for (int i = 0; i < processor.getErrors().size(); i++)
			System.err.println(processor.getErrors().get(i));
		for (int i = 0; i < processor.getWarnings().size(); i++)
			System.err.println(processor.getWarnings().get(i));
		assertEquals(0, processor.getErrors().size());
		assertEquals(0, processor.getWarnings().size());
	}

}
