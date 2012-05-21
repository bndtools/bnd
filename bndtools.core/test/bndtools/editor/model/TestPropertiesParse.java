package bndtools.editor.model;

import junit.framework.TestCase;


public class TestPropertiesParse extends TestCase {
    
    public void testEqualsNoWhitespace() {
        assertEquals("hello", PropertiesParser.getPropertyKey("hello=world"));
    }

    public void testColonNoWhitespace() {
        assertEquals("hello", PropertiesParser.getPropertyKey("hello:world"));
    }
    
    public void testLeadingWhitespace() {
        assertEquals("hello", PropertiesParser.getPropertyKey("   hello:world"));
    }
    
    public void testCommentLine() {
        assertEquals(null, PropertiesParser.getPropertyKey("  #hello=world"));
        assertEquals(null, PropertiesParser.getPropertyKey("  !hello=world"));
    }

    public void testTrailingWhitespace() {
        assertEquals("hello", PropertiesParser.getPropertyKey("   hello  :world"));
    }
    
    public void testEscapedWhitespace() {
        assertEquals("hel  lo", PropertiesParser.getPropertyKey("hel\\ \\ lo: world"));
    }
    
    public void testEscapedSeparator() {
        assertEquals("hell=o:world", PropertiesParser.getPropertyKey("hell\\=o\\:world=foo"));
    }
    
    public void testNoSeparator() {
        assertEquals("cheeses", PropertiesParser.getPropertyKey("cheeses"));
    }
    
    public void testEmptyLine() {
        assertEquals(null, PropertiesParser.getPropertyKey(""));
        assertEquals(null, PropertiesParser.getPropertyKey("   "));
        assertEquals(null, PropertiesParser.getPropertyKey("\t  "));
    }
    
}
