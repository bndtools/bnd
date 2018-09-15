package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class PropertiesTest extends TestCase {
	static <T> T notNull(T t) {
		assertNotNull(t);
		return t;
	}

	public static void testFlattening() throws Exception {
		Processor p = new Processor();
		p.setProperty("-versionpolicy", "${version;===;${@}}");
		p.setProperty("x", "x");
		p.setProperty("_x", "_x");

		Properties flattened = notNull(p.getFlattenedProperties());
		String x = notNull(flattened.getProperty("-versionpolicy"));
		assertTrue(x.contains("@"));
		notNull(flattened.getProperty("x"));
		assertNull(flattened.getProperty("_x"));
		assertEquals(2, flattened.size());
	}

	public static void testFilter() {
		Processor p1 = new Processor();
		p1.setProperty("dan", "bandera");
		p1.setProperty("susan", "sarandon");
		p1.setProperty("jon", "bostrom");

		Processor p2 = new Processor(p1);
		p2.setForceLocal(Arrays.asList("dan"));
		p2.setProperty("susan", "schwarze");

		assertNull(p2.getProperty("dan"));
		assertEquals("schwarze", p2.getProperty("susan"));
		assertEquals("bostrom", p2.getProperty("jon"));
	}

	public static void testUnicode() {
		StringBuilder sb = new StringBuilder();
		String s = "Loïc Cotonéa";
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 || c > 0x7F) {
				sb.append("\\u");
				sb.append(String.format("%04x", (int) c));
			} else {
				sb.append(c);
			}
		}
		System.err.println(sb);
	}

	public static void testSpacesAround() throws Exception {
		String test = "#comment\n" + "   abc    =   abc\r\n" + "def = def\n\r" + " ghi =               ghi\r"
			+ " jkl =               jkl";

		byte[] bytes = test.getBytes("ISO8859-1");
		ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
		Properties p = new Properties();
		p.load(bin);

		assertEquals("abc", p.get("abc"));
		assertEquals("def", p.get("def"));
		assertEquals("ghi", p.get("ghi"));
		assertEquals("jkl", p.get("jkl"));
	}

	public static void testInternationalCharacters() throws Exception {
		String test = "#comment\n" + "Namex=Lo\u00EFc Coton\u00E9a\n" + "Export-Package: *\n" + "Unicode=\\u0040\n"
			+ "NameAgain=Loïc Cotonéa";

		byte[] bytes = test.getBytes("ISO8859-1");
		ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
		Properties p = new Properties();
		p.load(bin);
		assertEquals("@", p.get("Unicode"));
		assertEquals("Lo\u00EFc Coton\u00E9a", p.get("Namex"));

		// Now test if we can make the round trip
		Builder b = new Builder();
		b.setProperties(p);
		b.addClasspath(IO.getFile("jar/asm.jar"));
		Jar jar = b.build();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.getManifest()
			.write(bout);

		bin = new ByteArrayInputStream(bout.toByteArray());
		Manifest m = new Manifest(bin);

		assertEquals("Lo\u00EFc Coton\u00E9a", m.getMainAttributes()
			.getValue("Namex"));
	}

	public static void testBadProperties() throws Exception {
		Analyzer analyzer = new Analyzer();
		analyzer.setPedantic(true);
		analyzer.setProperties(IO.getFile("test/test/badproperties.prop"));
		String s = analyzer.getProperty(Constants.IMPORT_PACKAGE);
		Parameters map = analyzer.parseHeader(s);
		analyzer.check("This is allowed in a properties file but not in bnd to prevent mistakes:",
			"No value specified for key: my.package",
			"Empty clause, usually caused by repeating a comma without any name field or by having space");
		assertEquals(2, map.size());
		assertTrue(map.containsKey("org.osgi.service.cm"));
		assertTrue(map.containsKey("org.osgi.util.tracker"));
	}

	public static void testProperties() throws Exception {
		Analyzer analyzer = new Analyzer();
		analyzer.setProperties(IO.getFile("test/test/variables.mf"));

		assertEquals("aQute.test", analyzer.getProperty("Header"));
		System.err.println("property " + analyzer.getProperty("Header"));
	}
}
