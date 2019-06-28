package test;

import aQute.bnd.osgi.Processor;
import junit.framework.TestCase;

public class SpringTest extends TestCase {

	/**
	 * Basic test to see if the analzyer works. We read the sprint-test1.xml and
	 * see if we can detect all the different packages.
	 *
	 * @throws Exception
	 */
	public static void testSimple() throws Exception {

		// InputStream in = getClass().getResourceAsStream("spring-test1.xml");
		// Set set = SpringComponent.analyze(in);
		// System.err.println(set);
		// assertTrue(set.contains("com.foo.one"));
		// assertTrue(set.contains("com.foo.one.one"));
		// assertTrue(set.contains("com.foo.one.two"));
		// assertTrue(set.contains("value_type"));
		// assertTrue(set.contains("a.b"));
		// assertTrue(set.contains("c.d"));
		// assertTrue(set.contains("e.f"));
		// assertTrue(set.contains("interfaces_1"));
		// assertTrue(set.contains("interfaces_2"));
		// assertFalse(set.contains("interfaces_3"));
		// assertFalse(set.contains("I-am-not-here"));
	}

	/**
	 * Now check if the plugin works, we create a dummy bundle and put the
	 * spring-test1.xml in the appropriate place. This means that the
	 * import-header contains all the the packages.
	 *
	 * @throws Exception public void testPlugin() throws Exception { Builder b =
	 *             new Builder(); b.setProperty(Analyzer.INCLUDE_RESOURCE,
	 *             "META-INF/spring/one.xml=test/test/spring-test1.xml");
	 *             b.setProperty(Analyzer.IMPORT_PACKAGE, "*");
	 *             b.setProperty(Analyzer.EXPORT_PACKAGE, "*");
	 *             b.setClasspath(new File[] { IO.getFile("jar/asm.jar") }); Jar
	 *             jar = b.build(); checkMessages(b,0,0); test(jar); }
	 */

	/**
	 * See what happens if we put the spring file in the wrong place. We should
	 * have no import packages.
	 */
	// public void testPluginWrongPlace() throws Exception {
	// Builder b = new Builder();
	// b.setProperty(Analyzer.INCLUDE_RESOURCE,
	// "META-INF/not-spring/one.xml=test/test/spring-test1.xml");
	// Jar jar = b.build();
	// checkMessages(b,0,2);
	// Manifest m = jar.getManifest();
	// assertNull(m.getMainAttributes().getValue(Analyzer.IMPORT_PACKAGE));
	// }
	//
	// void test(Jar jar) throws Exception {
	// Manifest m = jar.getManifest();
	// String header = m.getMainAttributes().getValue("Import-Package");
	// assertTrue(header.indexOf("com.foo.one") >= 0);
	// assertTrue(header.indexOf("com.foo.one.one") >= 0);
	// assertTrue(header.indexOf("com.foo.one.two") >= 0);
	// assertTrue(header.indexOf("value_type") >= 0);
	// assertTrue(header.indexOf("a.b") >= 0);
	// assertTrue(header.indexOf("c.d") >= 0);
	// assertTrue(header.indexOf("e.f") >= 0);
	// assertTrue(header.indexOf("interfaces_1") >= 0);
	// assertTrue(header.indexOf("interfaces_2") >= 0);
	// assertFalse(header.indexOf("interfaces_3") >= 0);
	// assertFalse(header.indexOf("I-am-not-here") >= 0);
	//
	// }

	public static void checkMessages(Processor processor, int errors, int warnings) {
		System.err.println("Errors:    " + processor.getErrors());
		System.err.println("Warnings:  " + processor.getWarnings());
		assertEquals(errors, processor.getErrors()
			.size());
		assertEquals(warnings, processor.getWarnings()
			.size());

	}
}
