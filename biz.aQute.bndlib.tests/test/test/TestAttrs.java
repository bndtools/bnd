package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Attrs;

public class TestAttrs {

	@Test
	public void testAttrs() {
		Attrs attrs = new Attrs();
		attrs.put("a", "aa");
		attrs.put("a:", "ab");
		attrs.put("along:Long", "123");
		attrs.put("aversion:Version", "1.2.3");

		assertEquals("aa", attrs.get("a"));
		assertEquals("ab", attrs.get("a:"));
		assertEquals("123", attrs.get("along"));
		assertEquals("1.2.3", attrs.get("aversion"));
	}
}
