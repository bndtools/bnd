package aQute.libg.glob;

import junit.framework.TestCase;

public class GlobTest extends TestCase {

	public void testSimple() {
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

	public void testCurlies() {
		Glob glob = new Glob("xx{abc,def,ghi}xx");
		assertTrue(glob.matcher("xxabcxx")
			.matches());

		glob = new Glob("*{.groovy,.java}");
		assertTrue(glob.matcher("FooBar.java")
			.matches());
		assertTrue(glob.matcher("FooBar.groovy")
			.matches());
	}

	public void testOr() {
		Glob glob = new Glob("abc|def|ghi");
		assertTrue(glob.matcher("abc")
			.matches());
	}

	public void testGroups() {
		Glob glob = new Glob("xx(abc|def|ghi)xx");
		assertTrue(glob.matcher("xxabcxx")
			.matches());

		glob = new Glob("*(?:.groovy|.java)");
		assertTrue(glob.matcher("FooBar.java")
			.matches());
		assertTrue(glob.matcher("FooBar.groovy")
			.matches());
	}

	public void testChar() {
		Glob glob = new Glob("xx[abc]xx");
		assertTrue(glob.matcher("xxbxx")
			.matches());
		assertTrue(glob.matcher("xxcxx")
			.matches());

		glob = new Glob("xx[abc]{2}xx");
		assertTrue(glob.matcher("xxbbxx")
			.matches());
		assertTrue(glob.matcher("xxcaxx")
			.matches());
		assertFalse(glob.matcher("xxcxx")
			.matches());

		glob = new Glob("xx[abc]?xx");
		assertTrue(glob.matcher("xxbxx")
			.matches());
		assertTrue(glob.matcher("xxxx")
			.matches());
		assertFalse(glob.matcher("xxacxx")
			.matches());
	}

	public void testGroupQuantifier() {
		Glob glob = new Glob("xx(abc|def|ghi){2}xx");
		assertFalse(glob.matcher("xxabcxx")
			.matches());
		assertTrue(glob.matcher("xxabcabcxx")
			.matches());
		assertTrue(glob.matcher("xxabcdefxx")
			.matches());
		assertFalse(glob.matcher("xxabcdefghixx")
			.matches());

		glob = new Glob("*(.groovy|.java)?");
		assertTrue(glob.matcher("FooBar.java")
			.matches());
		assertTrue(glob.matcher("FooBar.groovy")
			.matches());
		assertTrue(glob.matcher("FooBar")
			.matches());
	}

	public void testUrl() {
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
