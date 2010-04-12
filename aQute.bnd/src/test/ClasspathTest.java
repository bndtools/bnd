package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class ClasspathTest extends TestCase {

    /**
     * Test if we can refer to the jars on the classpath by their file name (
     * ignoring the path)
     * 
     * @throws Exception
     */
    public void testBundleClasspath() throws Exception {
        Builder b = new Builder();
        b.setProperty("Include-Resource", "bin=bin");
        b.setProperty("Bundle-Classpath", "bin");

        Jar jar = b.build();
        assertNotNull(jar.getResource("bin/aQute/lib/osgi/Verifier.class")); // from
                                                                    // test.jar
    }
    
	/**
	 * Test if we can refer to the jars on the classpath by their file name (
	 * ignoring the path)
	 * 
	 * @throws Exception
	 */
	public void testFindJarOnClasspath() throws Exception {
		Properties p = new Properties();
		p.put("Include-Resource", "tb1.jar, @test.jar");

		Builder b = new Builder();
		b.setClasspath(new String[] { "src", "src/test/test.jar",
				"src/test/tb1.jar" });
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
	public void testSimple() throws Exception {
		Properties p = new Properties();
		p.put("-classpath", new File("src/test/test.jar").toURL().toString());
		p.put("Import-Package", "*");
		p.put("Export-Package", "aQute.bean");

		Builder b = new Builder();
		b.setClasspath(new String[] { "src" });
		b.setProperties(p);
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		String importPackage = m.getMainAttributes().getValue("Import-Package");
		assertEquals("aQute.bean;version=\"1.0\"", importPackage);
	}

}
