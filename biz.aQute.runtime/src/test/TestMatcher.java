package test;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import aQute.junit.runtime.minifw.*;

public class TestMatcher extends TestCase {

    public void testSimple() {
        assertTrue(Context.matches("abc.def", "*.def"));
        assertTrue(Context.matches(".def", "*.def"));
        assertTrue(Context.matches("abc.def", "abc*.def"));
        assertTrue(Context.matches("abc.def", "abc*****.def"));
        assertTrue(Context.matches("abcdef.def", "abc*****.def"));
        assertFalse(Context.matches("def.def", "abc*****.def"));
        assertTrue(Context.matches("abc.def", "abc.def*"));
        assertTrue(Context.matches("abc.defxxx", "abc.def*"));
        assertTrue(Context.matches("abc.defxxx", "abc.def*x"));
        assertTrue(Context.matches("abc.defxxx", "abc*.def*x"));
        assertTrue(Context.matches("abcx.defxxx", "abc*.def*x"));
        assertFalse(Context.matches("abcx.defxxx", "abc.def*x"));
    }

    public void testContext() throws Exception {

        assertContents("/org/osgi/service", "*.class", true,
                "org/osgi/service/log/LogService.class", true);
        assertContents("/org/osgi/service/", "*.class", true,
                "org/osgi/service/log/LogService.class", true);
        assertContents("org/osgi/service", "*.class", true,
                "org/osgi/service/log/LogService.class", true);
        assertContents("org/osgi/service/log", "*.class", false,
                "org/osgi/service/log/LogService.class", true);
        assertContents("org/osgi/service/", "*.class", false,
                "org/osgi/service/log/LogService.class", false);
        assertContents("org/osgi/service/", null, true,
                "org/osgi/service/log/LogService.class", true);
        assertContents("org/osgi/service/", "LogService*", true,
                "org/osgi/service/log/LogService.class", true);
        assertContents("org/osgi/service/", "LogService.class", true,
                "org/osgi/service/log/LogService.class", true);
                
    }

    void assertContents(String path, String pattern, boolean recurse,
            String member, boolean mustfind) throws IOException {
        Context context = new Context(null, getClass().getClassLoader(), 1,
                "src/test/osgi.jar");
        Enumeration e = context.findEntries(path, "*.class", recurse);
        boolean found = false;
        while (e.hasMoreElements()) {
            URL url = (URL) e.nextElement();
            if (url.getPath().endsWith(member)) {
                found = true;
                
                break;
            }
        }
        if (found == mustfind)
            return;

        if (mustfind)
            fail("No such member: " + member + " in " + pattern);
        else
            fail("Unexpected member: " + member + " in " + pattern);
    }
}
