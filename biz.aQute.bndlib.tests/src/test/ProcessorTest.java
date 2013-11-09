package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

public class ProcessorTest extends TestCase {

	public static void testPlugins() {

	}
	
	public void testFixupMessages() throws IOException {
		Processor p = new Processor();
		p.setTrace(true);
		
		p.error("abc");
		assertFalse(p.isOk());

		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;restrict:=warning");
		assertEquals(1,p.getErrors().size());
		assertEquals(0,p.getWarnings().size());
		
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc");
		assertEquals(0,p.getErrors().size());
		assertEquals(0,p.getWarnings().size());
		
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=error");
		assertEquals(1,p.getErrors().size());
		assertEquals(0,p.getWarnings().size());
		
		p.clear();
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=warning");
		assertEquals(0,p.getErrors().size());
		assertEquals(1,p.getWarnings().size());
		
		p.clear();
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def");
		assertEquals("def", p.getErrors().get(0));
		assertEquals(0,p.getWarnings().size());
		
		p.clear();
		p.setProperty(Constants.FIXUPMESSAGES, "'abc def\\s*ghi';is:=warning");
		p.error("abc def  \t\t   ghi");
		assertEquals(0,p.getErrors().size());
		assertEquals(1,p.getWarnings().size());
		
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def;is:=warning");
		assertEquals("def", p.getWarnings().get(0));
		assertEquals(0,p.getErrors().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;restrict:=error");
		assertEquals(0,p.getErrors().size());
		assertEquals(1,p.getWarnings().size());
		
		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc");
		assertEquals(0,p.getErrors().size());
		assertEquals(0,p.getWarnings().size());
		
		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=warning");
		assertEquals(0,p.getErrors().size());
		assertEquals(1,p.getWarnings().size());
		
		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=error");
		assertEquals(1,p.getErrors().size());
		assertEquals(0,p.getWarnings().size());
		
		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def");
		assertEquals("def", p.getWarnings().get(0));
		assertEquals(0,p.getErrors().size());
		
		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def;is:=error");
		assertEquals("def", p.getErrors().get(0));
		assertEquals(0,p.getWarnings().size());
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
