package test;

import junit.framework.*;
import aQute.libg.glob.*;

public class GlobTest extends TestCase {
	public void testSimple() {
		Glob glob = new Glob("*foo*");
		assertTrue(glob.matcher("foo").find());
		assertTrue(glob.matcher("food").find());
		assertTrue(glob.matcher("Xfoo").find());
		assertTrue(glob.matcher("XfooY").find());
		assertFalse(glob.matcher("ood").find());
	}
	
	public void testUrl() {
		Glob glob;
		
		glob = new Glob("http://www.example.com/*");
		assertTrue(glob.matcher("http://www.example.com/repository.xml").find());
		assertFalse(glob.matcher("https://www.example.com/repository.xml").find());
		
		glob = new Glob("http://*.example.com/*");
		assertTrue(glob.matcher("http://www.example.com/repository.xml").find());
		assertTrue(glob.matcher("http://foo.example.com/repository.xml").find());
		assertFalse(glob.matcher("http://example.com/repository.xml").find());
		assertFalse(glob.matcher("http://foo.exampleXcom/repository.xml").find());
	}
}
