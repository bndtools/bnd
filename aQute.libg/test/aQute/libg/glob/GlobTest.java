package aQute.libg.glob;

import junit.framework.TestCase;

public class GlobTest extends TestCase {

	public static void testSimple() {
		Glob glob = new Glob("*foo*");
		assertTrue(glob.matcher("foo")
			.matches());
		assertTrue(glob.matcher("food")
			.matches());
		assertTrue(glob.matcher("Xfoo")
			.matches());
		assertTrue(glob.matcher("XfooY")
			.matches());
		assertFalse(glob.matcher("ood")
			.matches());
	}

	public static void testCurlies() {
		Glob glob = new Glob("xx{abc,def,ghi}xx");
		assertTrue(glob.matcher("xxabcxx").find());
		Glob g2 = new Glob("*.{groovy,java}");
		assertTrue(g2.matcher("FooBar.java").find());
		assertTrue(g2.matcher("FooBar.groovy").find());
	}

	public static void testUrl() {
		Glob glob;

		glob = new Glob("http://www.example.com/*");
		assertTrue(glob.matcher("http://www.example.com/repository.xml")
			.matches());
		assertFalse(glob.matcher("https://www.example.com/repository.xml")
			.matches());

		glob = new Glob("http://*.example.com/*");
		assertTrue(glob.matcher("http://www.example.com/repository.xml")
			.matches());
		assertTrue(glob.matcher("http://foo.example.com/repository.xml")
			.matches());
		assertFalse(glob.matcher("http://example.com/repository.xml")
			.matches());
		assertFalse(glob.matcher("http://foo.exampleXcom/repository.xml")
			.matches());
	}
}
