package test;

import junit.framework.*;
import aQute.lib.osgi.*;

public class FilterTest extends TestCase {

    public void testFilter() {
        Verifier v = new Verifier();
        v.verifyFilter("(org.osgi.framework.windowing.system=xyz)");
        System.out.println(v.getErrors());
        System.out.println(v.getWarnings());
        assertEquals(0, v.getErrors().size());
        assertEquals(0, v.getWarnings().size());
    }
}
