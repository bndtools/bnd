package aQute.lib.properties;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;

public class PropertiesParserTest extends TestCase {

	static String t1 = "abc=1\n"
			+ "def:2    \t\n"
			+ "ghi 3\n"
			+ "jkl = 4                 \t\n"
			+ "mno {5}\n"
			+ "pqr= {\n1\n2\n3\n}\n"
			+ "[section]\n"
			+ "stv =6";
	
	public void testSimple() throws IOException, URISyntaxException {
		Properties p = PropertiesParser.parse(new StringReader(t1), "", null);
		assertEquals("1", p.getProperty("abc"));
		assertEquals("2", p.getProperty("def"));
		assertEquals("3", p.getProperty("ghi"));
		assertEquals("4", p.getProperty("jkl"));
		assertEquals("5", p.getProperty("mno"));
		assertEquals("1\n2\n3", p.getProperty("pqr"));
		assertEquals("6", p.getProperty("section.stv"));
		System.out.println(p.getProperty("$$$ERRORS"));
		assertNull( p.getProperty("$$$ERRORS"));
	}
}
