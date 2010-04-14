package test;

import junit.framework.*;
import aQute.junit.osgi.*;

public class TestChanges extends TestCase {

    public void testReplace( ) {
        assertEquals("abcABCabc", Activator.replace("abc%sabc","%s", "ABC"));
    }
    
    public void testSplit( ) {
        String [] splitted = Activator.split("a , b,c,d,                            e", ",");
        assertEquals(5, splitted.length);
        
        assertEquals("a", splitted[0]);
        assertEquals("b", splitted[1]);
        assertEquals("c", splitted[2]);
        assertEquals("d", splitted[3]);
        assertEquals("e", splitted[4]);
    }
    public void testSplit2( ) {
        String [] splitted = Activator.split("a abc babccabcdabc                            e", "abc");
        assertEquals(5, splitted.length);
        
        assertEquals("a", splitted[0]);
        assertEquals("b", splitted[1]);
        assertEquals("c", splitted[2]);
        assertEquals("d", splitted[3]);
        assertEquals("e", splitted[4]);
    }
}
