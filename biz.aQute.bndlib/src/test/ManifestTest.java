package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class ManifestTest extends TestCase {
   
    
    public void testNoManifest() throws Exception {
        Builder b = new Builder();
        b.setProperty("-nomanifest","true");
        b.setProperty("Export-Package","org.osgi.service.event.*");
        b.addClasspath(new File("jar/osgi.jar"));
        Jar jar = b.build();
        assertNull( jar.getResource("META-INF/MANIFEST.MF"));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jar.write(bout);
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        JarInputStream  in = new JarInputStream(bin);
        ZipEntry entry = in.getNextEntry();
        assertNotNull(entry);
        assertNull( entry.getExtra());
    }
    
    
    public void testNames() throws Exception {
        Manifest m = new Manifest();
        m.getMainAttributes().putValue("Manifest-Version", "1.0");
        m.getMainAttributes().putValue("x", "Loïc Cotonéa");
        m.getMainAttributes().putValue("y", "Loïc Cotonéa");
        m.getMainAttributes().putValue("z", "Loïc Cotonéa");
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Jar.writeManifest(m,bout);
        byte [] result = bout.toByteArray();
        
        System.out.println( new String(result));
    }
    

    public void testUTF8() throws Exception {
        Manifest m = new Manifest();
        m.getMainAttributes().putValue("Manifest-Version", "1.0");
        m.getMainAttributes().putValue("x", "Loïc Cotonéa");
        m.getMainAttributes().putValue("y", "Loïc Cotonéa");
        m.getMainAttributes().putValue("z", "Loïc Cotonéa");
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Jar.writeManifest(m,bout);
        byte [] result = bout.toByteArray();
        
        System.out.println( new String(result));
    }
    
	public void testQuotes() {
		Map<String,Map<String,String>> map = new HashMap<String,Map<String,String>>();
		Map<String,String> clause = new HashMap<String,String>();
		clause.put("version1", "0");
		clause.put("version2", "0.0");
		clause.put("version3", "\"0.0\"");
		clause.put("version4", "   \"0.0\"    ");
		clause.put("version5", "   0.0    ");
		map.put("alpha", clause);
		String s = Processor.printClauses(map,"");
		assertTrue( s.indexOf("version1=0")>=0);
		assertTrue( s.indexOf("version2=\"0.0\"")>=0);
		assertTrue( s.indexOf("version3=\"0.0\"")>=0);
		assertTrue( s.indexOf("version4=\"0.0\"")>=0);
		assertTrue( s.indexOf("version5=\"0.0\"")>=0);
	}
}
