package test;

import junit.framework.*;
import aQute.libg.qtokens.*;

public class TestQuotedTokenizer extends TestCase {

	public static void testNativeSeps() {
		String s[] = new QuotedTokenizer("x;c;d=4", ";,=", true).getTokens();
		assertEquals("Length", 7, s.length);
		assertEquals("x", s[0]);
		assertEquals(";", s[1]);
		assertEquals("c", s[2]);
		assertEquals(";", s[3]);
		assertEquals("d", s[4]);
		assertEquals("=", s[5]);
		assertEquals("4", s[6]);
	}

	public static void testSimple() {
		String s[] = new QuotedTokenizer("1.jar, 2.jar,    \t   3.jar", ",").getTokens();
		assertEquals("Length", 3, s.length);
		assertEquals("1.jar", s[0]);
		assertEquals("2.jar", s[1]);
		assertEquals("3.jar", s[2]);
	}

	public static void testQuoted() {
		String s[] = new QuotedTokenizer("'1 ,\t.jar'", ",").getTokens();
		assertEquals("Length", 1, s.length);
		assertEquals("1 ,\t.jar", s[0]);
	}

	public static void testWhiteSpace() {
		String s[] = new QuotedTokenizer("               1.jar,               2.jar         ", ",").getTokens();
		assertEquals("Length", 2, s.length);
		assertEquals("1.jar", s[0]);
		assertEquals("2.jar", s[1]);
	}

	public static void testMultipleSeps() {
		String s[] = new QuotedTokenizer("1.jar,,,,,,,,,,,    , ,2.jar", ",").getTokens();
		assertEquals("Length", 14, s.length);
		assertEquals("1.jar", s[0]);
		assertEquals("2.jar", s[13]);
	}

	public static void testNative() {
		String s[] = new QuotedTokenizer("x.dll;y.dll;abc=3;def=5;version=\"1.2.34,123\"", ";,=").getTokens();
		assertEquals("Length", 8, s.length);
		assertEquals("x.dll", s[0]);
		assertEquals("y.dll", s[1]);
		assertEquals("abc", s[2]);
		assertEquals("3", s[3]);
		assertEquals("def", s[4]);
		assertEquals("5", s[5]);
		assertEquals("version", s[6]);
		assertEquals("1.2.34,123", s[7]);
	}
	
	public static void testEscapedQuote() {
		QuotedTokenizer qt = new QuotedTokenizer("'\\'y'", ",");
		String s = qt.nextToken();
		assertEquals("'y", s);
	}
	
	public static void testExplicitEmptyStringTurnedToNull() {
		QuotedTokenizer qt = new QuotedTokenizer("literal=''", ";=,");
		qt.nextToken();
		assertNull(qt.nextToken());
	}
}
