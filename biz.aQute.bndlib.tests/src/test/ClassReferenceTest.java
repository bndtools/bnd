package test;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz.JAVA;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

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

	public void testSun_1_1() throws Exception {
		doit("sun_1_1");
	}

	public void testSun_1_2() throws Exception {
		doit("sun_1_2");
	}

	public void testSun_1_3() throws Exception {
		doit("sun_1_3");
	}

	public void testSun_1_4() throws Exception {
		doit("sun_1_4");
	}

	public void testSun_1_5() throws Exception {
		doit("sun_1_5");
	}

	public void testSun_jsr14() throws Exception {
		doit("sun_jsr14");
	}

	public void testSun_1_6() throws Exception {
		doit("sun_1_6");
	}

	public void testSun_1_7() throws Exception {
		doit("sun_1_7");
	}

	public void testSun_1_8() throws Exception {
		doit("sun_1_8");
	}

	public void testEclipse_1_1() throws Exception {
		doit("eclipse_1_1");
	}

	public void testEclipse_1_2() throws Exception {
		doit("eclipse_1_2");
	}

	public void testEclipse_1_3() throws Exception {
		doit("eclipse_1_3");
	}

	public void testEclipse_1_4() throws Exception {
		doit("eclipse_1_4");
	}

	public void testEclipse_1_5() throws Exception {
		doit("eclipse_1_5");
	}

	public void testEclipse_1_6() throws Exception {
		doit("eclipse_1_6");
	}

	public void testEclipse_1_7() throws Exception {
		doit("eclipse_1_7");
	}

	public void doit(String p) throws Exception {
		Properties properties = new Properties();
		properties.put("-classpath", "compilerversions/compilerversions.jar");
		System.out.println("compiler version " + p);
		Builder builder = new Builder();
		properties.put(Constants.EEPROFILE, "auto");
		properties.put("Export-Package", p);
		builder.setProperties(properties);
		Jar jar = builder.build();
		assertTrue(builder.check());
		JAVA highestEE = builder.getHighestEE();
		Map<String, Set<String>> profiles = highestEE.getProfiles();
		if (profiles != null) {
			System.out.println("profiles" + profiles);
			jar.getManifest()
				.write(System.out);
		}

		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		String imports = manifest.getMainAttributes()
			.getValue("Import-Package");
		assertTrue("Package " + p + "contains swing ref ", imports.contains("javax.swing"));
		assertFalse("Package " + p + "should not contain ClassRef", imports.contains("ClassRef"));
	}
}
