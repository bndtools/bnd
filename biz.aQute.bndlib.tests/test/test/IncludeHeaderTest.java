package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class IncludeHeaderTest {

	/**
	 * Test url includes props: a\ b\ c\ d last-props: end
	 */
	@Test
	public void testUrlIncludes() throws IOException {
		try (Analyzer a = new Analyzer()) {
			Properties p = new Properties();
			p.setProperty("a", "1");
			p.setProperty("-include", "file:test/test/includeheadertest.prop");
			a.setProperties(p);
			assertEquals("1", a.getProperty("a"));
			assertEquals("end", a.getProperty("last-props"));
			assertEquals("abcd", a.getProperty("props"));
		}
	}

	/**
	 * Test url includes
	 */
	@Test
	public void testUrlIncludes2() throws IOException {
		try (Analyzer a = new Analyzer()) {
			Properties p = new Properties();
			p.setProperty("a", "1");
			p.setProperty("-include", "jar:file:jar/osgi.jar/!/META-INF/MANIFEST.MF");
			a.setProperties(p);
			assertEquals("1", a.getProperty("a"));
			assertEquals("osgi", a.getProperty("Bundle-SymbolicName"));
		}
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

	@Test
	public void testTopBottom() throws Exception {
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setProperties(IO.getFile("test/test/include.bnd/top.bnd"));
			assertEquals("0.0.257", analyzer.getProperty("Bundle-Version"));
		}
	}

	@Test
	public void testPrecedence(@InjectTemporaryDirectory
	File tmp) throws Exception {
		IO.store("a=a.props\n", new File(tmp, "a.props"));
		IO.store("a=b.props\n", new File(tmp, "b.props"));

		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setBase(tmp);
			Properties x = new Properties();
			x.put("a", "x");
			x.put("-include", "a.props, b.props");
			analyzer.setProperties(x);
			assertEquals("b.props", analyzer.getProperty("a")); // from org
		}

		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setBase(tmp);
			Properties x = new Properties();
			x.put("a", "x");
			x.put("-include", "~a.props, b.props");
			analyzer.setProperties(x);
			assertEquals("b.props", analyzer.getProperty("a")); // from org
		}

		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setBase(tmp);
			Properties x = new Properties();
			x.put("a", "x");
			x.put("-include", "a.props, ~b.props");
			analyzer.setProperties(x);
			assertEquals("a.props", analyzer.getProperty("a")); // from org
		}

		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setBase(tmp);
			Properties x = new Properties();
			x.put("a", "x");
			x.put("-include", "~a.props, ~b.props");
			analyzer.setProperties(x);
			assertEquals("x", analyzer.getProperty("a")); // from org
		}
	}

	@Test
	public void testAbsentIncludes() throws IOException {
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setBase(IO.getFile("test/test"));
			Properties p = new Properties();
			p.put("-include", "-iamnotthere.txt");
			analyzer.setProperties(p);
			System.err.println(analyzer.getErrors());
			assertEquals(0, analyzer.getErrors()
				.size());
		}
	}

	@Test
	public void testIncludeWithProperty(@InjectTemporaryDirectory
		File tmp) throws IOException {
		IO.store("IncludeHeaderTest: yes\n\r" + //
			"a: 2\n\r" + //
			"b: ${a}\n\r", new File(tmp, "includeheadertest.txt"));
		try (Processor parent = new Processor();
			Analyzer analyzer = new Analyzer(parent)) {
			parent.setProperty("home", tmp.getAbsolutePath());
			analyzer.setBase(tmp);
			Properties p = new Properties();
			p.put("a", "1");
			p.put("-include", "-iamnotthere.txt, ${home}/includeheadertest.txt");
			analyzer.setProperties(p);
			String value = analyzer.getProperty("IncludeHeaderTest");
			assertEquals("yes", value);
			assertEquals("2", analyzer.getProperty("a"));
			assertEquals("2", analyzer.getProperty("b"));
			assertEquals(0, analyzer.getErrors()
				.size());
		}
	}

	@Test
	public void testIncludeHeader() throws IOException {
		try (Analyzer analyzer = new Analyzer()) {
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

}
