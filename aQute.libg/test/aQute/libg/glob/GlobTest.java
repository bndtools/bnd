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
