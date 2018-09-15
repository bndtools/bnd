package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import aQute.bnd.osgi.Analyzer;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class IncludeHeaderTest extends TestCase {

	/**
	 * Test url includes props: a\ b\ c\ d last-props: end
	 */
	public static void testUrlIncludes() throws IOException {
		Analyzer a = new Analyzer();
		Properties p = new Properties();
		p.setProperty("a", "1");
		p.setProperty("-include", "file:test/test/includeheadertest.prop");
		a.setProperties(p);
		assertEquals("1", a.getProperty("a"));
		assertEquals("end", a.getProperty("last-props"));
		assertEquals("abcd", a.getProperty("props"));
	}

	/**
	 * Test url includes
	 */
	public static void testUrlIncludes2() throws IOException {
		Analyzer a = new Analyzer();
		Properties p = new Properties();
		p.setProperty("a", "1");
		p.setProperty("-include", "jar:file:jar/osgi.jar/!/META-INF/MANIFEST.MF");
		a.setProperties(p);
		assertEquals("1", a.getProperty("a"));
		assertEquals("osgi", a.getProperty("Bundle-SymbolicName"));
	}

	// public void testMavenInclude() throws Exception {
	// String b = "pom.modelVersion=b\n";
	// File bb = new File("b.props");
	// write(bb, b );
	// bb.deleteOnExit();
	//
	// Analyzer analyzer = new Analyzer();
	// Properties x = new Properties();
	// x.put("pom.modelVersion", "set");
	// x.put("pom.scope.test", "set");
	// x.put("-include", "~maven/pom.xml,b.props");
	// analyzer.setProperties(x);
	// System.err.println(analyzer.getErrors());
	// System.err.println(analyzer.getWarnings());
	// assertEquals("b", analyzer.getProperty("pom.modelVersion")); // from b
	// assertEquals("org.apache.felix.metatype",
	// analyzer.getProperty("pom.artifactId")); // from pom
	// assertEquals("org.apache.felix", analyzer.getProperty("pom.groupId")); //
	// from parent pom
	// assertEquals("set", analyzer.getProperty("pom.scope.test")); // Set
	// }

	public static void testTopBottom() throws Exception {
		Analyzer analyzer = new Analyzer();
		analyzer.setProperties(IO.getFile("test/test/include.bnd/top.bnd"));
		assertEquals("0.0.257", analyzer.getProperty("Bundle-Version"));
	}

	public static void testPrecedence() throws Exception {
		File base = IO.getFile("test/test");
		String a = "a=a.props\n";
		String b = "a=b.props\n";
		File aa = new File(base, "a.props");
		File bb = new File(base, "b.props");
		write(aa, a);
		write(bb, b);

		Analyzer analyzer = new Analyzer();
		analyzer.setBase(base);
		Properties x = new Properties();
		x.put("a", "x");
		x.put("-include", "a.props, b.props");
		analyzer.setProperties(x);
		assertEquals("b.props", analyzer.getProperty("a")); // from org

		analyzer = new Analyzer();
		analyzer.setBase(base);
		x = new Properties();
		x.put("a", "x");
		x.put("-include", "~a.props, b.props");
		analyzer.setProperties(x);
		assertEquals("b.props", analyzer.getProperty("a")); // from org

		analyzer = new Analyzer();
		analyzer.setBase(base);
		x = new Properties();
		x.put("a", "x");
		x.put("-include", "a.props, ~b.props");
		analyzer.setProperties(x);
		assertEquals("a.props", analyzer.getProperty("a")); // from org

		analyzer = new Analyzer();
		analyzer.setBase(base);
		x = new Properties();
		x.put("a", "x");
		x.put("-include", "~a.props, ~b.props");
		analyzer.setProperties(x);
		assertEquals("x", analyzer.getProperty("a")); // from org

		aa.delete();
		bb.delete();
	}

	private static void write(File file, String b) throws Exception {
		FileOutputStream out = new FileOutputStream(file);
		out.write(b.getBytes());
		out.close();
	}

	public static void testAbsentIncludes() throws IOException {
		Analyzer analyzer = new Analyzer();
		analyzer.setBase(IO.getFile("test/test"));
		Properties p = new Properties();
		p.put("-include", "-iamnotthere.txt");
		analyzer.setProperties(p);
		System.err.println(analyzer.getErrors());
		assertEquals(0, analyzer.getErrors()
			.size());
	}

	public static void testIncludeWithProperty() throws IOException {
		File home = new File(System.getProperty("user.home"));
		File include = new File(home, "includeheadertest.txt");
		try {
			FileOutputStream fw = new FileOutputStream(include);
			fw.write("IncludeHeaderTest: yes\n\r".getBytes());
			fw.write("a: 2\n\r".getBytes());
			fw.write("b: ${a}\n\r".getBytes());
			fw.close();
			Analyzer analyzer = new Analyzer();
			analyzer.setBase(IO.getFile("test/test"));
			Properties p = new Properties();
			p.put("a", "1");
			p.put("-include", "-iamnotthere.txt, ${user.home}/includeheadertest.txt");
			analyzer.setProperties(p);
			String value = analyzer.getProperty("IncludeHeaderTest");
			assertEquals("yes", value);
			assertEquals("2", analyzer.getProperty("a"));
			assertEquals("2", analyzer.getProperty("b"));
			assertEquals(0, analyzer.getErrors()
				.size());
		} finally {
			include.delete();
		}
	}

	public static void testIncludeHeader() throws IOException {
		Analyzer analyzer = new Analyzer();
		analyzer.setBase(IO.getFile("test/test"));
		Properties p = new Properties();
		p.put("a", "1");
		p.put("-include", "includeheadertest.mf, includeheadertest.prop");
		analyzer.setProperties(p);
		System.err.println(analyzer.getProperties());
		assertEquals("1", analyzer.getProperty("a"));
		assertEquals("end", analyzer.getProperty("last-props"));
		assertEquals("end", analyzer.getProperty("last-manifest"));
		assertEquals("abcd", analyzer.getProperty("manifest"));
		assertEquals("abcd", analyzer.getProperty("props"));
		assertEquals("1", analyzer.getProperty("test"));
	}

}
