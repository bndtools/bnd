package test;

import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

@SuppressWarnings("resource")
public class ClassReferenceTest extends TestCase {
	class Inner {

	}

	static {
		System.err.println(Inner.class);
	}

	/**
	 * We create a JAR with the test.classreferenc.ClassReference class. This
	 * class contains a javax.swing.Box.class reference Prior to Java 1.5, this
	 * was done in a silly way that is handled specially. After 1.5 it is a
	 * normal reference.
	 * 
	 * @throws Exception
	 */
	public static void testReference() throws Exception {
		Properties properties = new Properties();
		properties.put("-classpath", "compilerversions/compilerversions.jar");
		String[] packages = {
				"sun_1_1", "sun_1_2", "sun_1_3", "sun_1_4", "sun_1_5", "sun_jsr14", "sun_1_6", "sun_1_7", "sun_1_8",//
				"eclipse_1_1", "eclipse_1_2", "eclipse_1_3", "eclipse_1_4",
				"eclipse_1_5", "eclipse_1_6", "eclipse_jsr14", "eclipse_1_7"
		};
		for (int i = 0; i < packages.length; i++) {
			System.out.println("compiler version " + packages[i]);
			Builder builder = new Builder();
			properties.put("Export-Package", packages[i]);
			builder.setProperties(properties);
			Jar jar = builder.build();
			assertTrue(builder.check());
			Manifest manifest = jar.getManifest();
			String imports = manifest.getMainAttributes().getValue("Import-Package");
			assertTrue("Package " + packages[i] + "contains swing ref", imports.indexOf("javax.swing") >= 0);
			assertFalse("Package " + packages[i] + "should not contain ClassRef", imports.indexOf("ClassRef") >= 0);
		}
	}
}
