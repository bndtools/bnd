package test;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

import aQute.junit.runtime.*;

public class TestFramework extends TestCase {

    public void testSimple() throws Exception {
        Properties p = new Properties();
        p.setProperty("noframework", "true");
        GenericFramework gfw = new GenericFramework(p);
     
        File f= new File("../aQute.bnd/jar/osgi.jar").getAbsoluteFile();
        gfw.addBundle( f );
        gfw.activate();
        
        Bundle b = gfw.getBundle(f.toString());
        assertNotNull(b.loadClass("org.osgi.service.log.LogService"));
    }
}
