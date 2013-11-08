package test;

import junit.framework.*;
import aQute.bnd.osgi.*;

public class ProcessorTest extends TestCase {

	public static void testPlugins() {

	}
	
	public void testFixupMessages() {
		Processor p = new Processor();
		p.error("abc");
		assertFalse(p.isOk());
		
		p.setProperty(Constants.FIXUPMESSAGES, "abc");
		assertTrue(p.isOk());
		
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace=def");
		assertFalse(p.isOk());
		assertEquals("def", p.getErrors().get(0));
		p.close();
	}

	public static void testDuplicates() {
		assertEquals("", Processor.removeDuplicateMarker("~"));

		assertTrue(Processor.isDuplicate("abc~"));
		assertTrue(Processor.isDuplicate("abc~~~~~~~~~"));
		assertTrue(Processor.isDuplicate("~"));
		assertFalse(Processor.isDuplicate(""));
		assertFalse(Processor.isDuplicate("abc"));
		assertFalse(Processor.isDuplicate("ab~c"));
		assertFalse(Processor.isDuplicate("~abc"));

		assertEquals("abc", Processor.removeDuplicateMarker("abc~"));
		assertEquals("abc", Processor.removeDuplicateMarker("abc~~~~~~~"));
		assertEquals("abc", Processor.removeDuplicateMarker("abc"));
		assertEquals("ab~c", Processor.removeDuplicateMarker("ab~c"));
		assertEquals("~abc", Processor.removeDuplicateMarker("~abc"));
		assertEquals("", Processor.removeDuplicateMarker(""));
		assertEquals("", Processor.removeDuplicateMarker("~~~~~~~~~~~~~~"));
	}

	public static void appendPathTest() throws Exception {
		assertEquals("a/b/c", Processor.appendPath("", "a/b/c/"));
		assertEquals("a/b/c", Processor.appendPath("", "/a/b/c"));
		assertEquals("a/b/c", Processor.appendPath("/", "/a/b/c/"));
		assertEquals("a/b/c", Processor.appendPath("a", "b/c/"));
		assertEquals("a/b/c", Processor.appendPath("a", "b", "c"));
		assertEquals("a/b/c", Processor.appendPath("a", "b", "/c/"));
		assertEquals("a/b/c", Processor.appendPath("/", "a", "b", "/c/"));
		assertEquals("a/b/c", Processor.appendPath("////////", "////a////b///c//"));

	}
}
