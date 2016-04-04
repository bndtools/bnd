package aQute.lib.strings;

import junit.framework.TestCase;

public class StringsTest extends TestCase {

	public void testTrim() {
		assertEquals("", Strings.trim(""));
		assertEquals("", Strings.trim("    "));
		assertEquals("", Strings.trim("\r\n\r\n\t\f\n\r"));
		assertEquals("a", Strings.trim("  a "));
		assertEquals("a", Strings.trim("  a\n\r"));
		assertEquals("a", Strings.trim("\r\n\r\na\t\f\n\r"));
		assertEquals("a b", Strings.trim("\r\n\r\na b\t\f\n\r"));
	}
}
