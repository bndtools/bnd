package test;

import junit.framework.*;
import aQute.lib.osgi.*;

public class ProcessorTest extends TestCase{

	
	public void testPlugins() {
		
	}
	
	
    public void testDuplicates() {
        assertEquals("", Processor.removeDuplicateMarker("~") );
        
        assertTrue( Processor.isDuplicate("abc~"));
        assertTrue( Processor.isDuplicate("abc~~~~~~~~~"));
        assertTrue( Processor.isDuplicate("~"));
        assertFalse( Processor.isDuplicate(""));
        assertFalse( Processor.isDuplicate("abc"));
        assertFalse( Processor.isDuplicate("ab~c"));
        assertFalse( Processor.isDuplicate("~abc"));
        
        assertEquals("abc", Processor.removeDuplicateMarker("abc~") );
        assertEquals("abc", Processor.removeDuplicateMarker("abc~~~~~~~") );
        assertEquals("abc", Processor.removeDuplicateMarker("abc") );
        assertEquals("ab~c", Processor.removeDuplicateMarker("ab~c") );
        assertEquals("~abc", Processor.removeDuplicateMarker("~abc") );
        assertEquals("", Processor.removeDuplicateMarker("") );
        assertEquals("", Processor.removeDuplicateMarker("~~~~~~~~~~~~~~") );
    }
    
    
    public void appendPathTest() throws Exception {
        assertEquals("a/b/c", Processor.appendPath("","a/b/c/"));
        assertEquals("a/b/c", Processor.appendPath("","/a/b/c"));
        assertEquals("a/b/c", Processor.appendPath("/","/a/b/c/"));
        assertEquals("a/b/c", Processor.appendPath("a","b/c/"));
        assertEquals("a/b/c", Processor.appendPath("a","b","c"));
        assertEquals("a/b/c", Processor.appendPath("a","b","/c/"));
        assertEquals("a/b/c", Processor.appendPath("/","a","b","/c/"));
        assertEquals("a/b/c", Processor.appendPath("////////","////a////b///c//"));
        
    }
}
