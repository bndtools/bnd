package aQute.lib.utf8properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Test if we properly can read
 * @author aqute
 *
 */
public class UTF8PropertiesTest extends TestCase {
	String trickypart = "\u00A0\u00A1\u00A2\u00A3\u00A4\u00A5\u00A6\u00A7\u00A8\u00A9\u00AA\u00AB\u00AC\u00AD\u00AE\u00AF"
			+ "\u00B0\u00B1\u00B2\u00B3\u00B4\u00B5\u00B6\u00B7\u00B8\u00B9\u00BA\u00BB\u00BC\u00BD\u00BE\u00BF"
			+ "\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C6\u00C7\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE\u00CF"
			+ "\u00D0\u00D1\u00D2\u00D3\u00D4\u00D5\u00D6\u00D7\u00D8\u00D9\u00DA\u00DB\u00DC\u00DD\u00DE\u00DF"
			+ "\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u00E6\u00E7\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF"
			+ "\u00F0\u00F1\u00F2\u00F3\u00F4\u00F5\u00F6\u00F7\u00F8\u00F9\u00FA\u00FB\u00FC\u00FD\u00FE\u00FF";
	
	
	public void testBackslashEncodingWithReader() throws IOException {
		Properties p = new UTF8Properties();
		p.load( new StringReader("x=abc \\\\ def\n"));
		assertEquals("abc \\ def", p.get("x"));
	}
	public void testISO8859Encoding() throws IOException {
		Properties p = new UTF8Properties();
		p.load( new ByteArrayInputStream(("x="+trickypart+"\n").getBytes("ISO-8859-1")));
		assertEquals(trickypart, p.get("x"));
	}
	
	public void testUTF8Encoding() throws IOException {
		Properties p = new UTF8Properties();
		p.load( new ByteArrayInputStream(("x="+trickypart+"\n").getBytes("UTF-8")));
		assertEquals(trickypart, p.get("x"));
	}
	
	public void testShowUTF8PropertiesDoNotSkipBackslash() throws IOException {
		Properties p = new UTF8Properties();
		p.load( new ByteArrayInputStream("x=abc \\ def\n".getBytes("UTF-8")));
		assertEquals("abc  def", p.get("x"));
	}
	
	public void testShowPropertiesSkipBackslash() throws IOException {
		Properties p = new Properties();
		p.load( new StringReader("x=abc \\ def\n"));
		assertEquals("abc  def", p.get("x"));
	}
	
	public void testWriteWithoutComment() throws IOException {
		UTF8Properties p = new UTF8Properties();
		p.put("Foo", "Foo");
		ByteArrayOutputStream bout  = new ByteArrayOutputStream();
		p.store(bout);
		String s = new String(bout.toByteArray(), "UTF-8");
		assertFalse(s.startsWith("#"));
	}
	
}
