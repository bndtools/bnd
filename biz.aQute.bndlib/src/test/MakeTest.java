package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.osgi.*;

/**
 * Tests the make functionality.
 * 
 * @author aqute
 *
 */
public class MakeTest extends TestCase {
    
    /**
     * Test a make plugin
     */
    
    public void testMakePlugin() throws Exception {
        Builder b = new Builder();
        b.setProperty("Export-Package", "*");
        b.setProperty("Include-Resource", "jar/asm.jar.md5");
        b.setProperty("-make", "(*).md5;type=md5;file=$1");        
        b.setProperty("-plugin", "test.make.MD5");
        b.addClasspath( new File("jar/osgi.jar"));
        Jar jar = b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());
        assertNotNull( jar.getResource("asm.jar.md5"));
    }
    
    /**
     * Check if we can get a resource through the make copy facility.
     * 
     * @throws Exception
     */
    public void testCopy() throws Exception {
        Builder bmaker = new Builder();
        Properties p = new Properties();
        p.setProperty("-resourceonly","true");
        p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
        p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd, (*).jar;type=copy;from=jar/$1.jar");
        p.setProperty("Include-Resource", "asm.jar,xyz=asm.jar");
        bmaker.setProperties(p);
        Jar jar = bmaker.build();
        assertNotNull(jar.getResource("asm.jar"));
        assertNotNull(jar.getResource("xyz"));
        report(bmaker);
        
    }
    
    
 
    /**
     * Check if we can create a JAR recursively
     * 
     * @throws Exception
     */
    public void testJarInJarInJar() throws Exception {
        Builder bmaker = new Builder();
        Properties p = new Properties();
        p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
        p.setProperty("-resourceonly","true");
        p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
        p.setProperty("Include-Resource", "makesondemand.jar");
        bmaker.setProperties(p);
        bmaker.setClasspath( new String[] {"bin"});
        Jar jar = bmaker.build();
        JarResource resource = (JarResource) jar.getResource("makesondemand.jar");
        assertNotNull(resource);
        
        jar = resource.getJar();
        resource = (JarResource) jar.getResource("ondemand.jar");
        assertNotNull(resource);
        
        report(bmaker);        
    }
    /**
     * Check if we can create a jar on demand through the make
     * facility with a new name.
     * 
     * @throws Exception
     */
    public void testComplexOnDemand() throws Exception {
        Builder bmaker = new Builder();
        Properties p = new Properties();
        p.setProperty("-resourceonly","true");
        p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
        p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
        p.setProperty("Include-Resource", "www/xyz.jar=ondemand.jar");
        bmaker.setProperties(p);
        bmaker.setClasspath( new String[] {"bin"});
        Jar jar = bmaker.build();
        Resource resource =jar.getResource("www/xyz.jar");
        assertNotNull(resource);
        assertTrue( resource instanceof JarResource );
        report(bmaker);
        
    }
    void report(Processor processor) {
        System.out.println();
        for ( int i=0; i<processor.getErrors().size(); i++ )
            System.out.println(processor.getErrors().get(i));
        for ( int i=0; i<processor.getWarnings().size(); i++ )
            System.out.println(processor.getWarnings().get(i));
        assertEquals(0, processor.getErrors().size());
        assertEquals(0, processor.getWarnings().size());
    }

}
