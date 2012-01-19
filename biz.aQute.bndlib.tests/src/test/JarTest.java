package test;

import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class JarTest extends TestCase {

    public void testNoManifest() throws Exception {
        Jar jar = new Jar("dot");
        jar.setManifest(new Manifest());
        jar.setDoNotTouchManifest();
        jar.putResource("a/b", new FileResource(new File("test/bnd.jar")));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jar.write(bout);

        Jar jin = new Jar("dotin", new ByteArrayInputStream(bout.toByteArray()));
        Resource m = jin.getResource("META-INF/MANIFEST.MF");
        assertNull(m);
        Resource r = jin.getResource("a/b");
        assertNotNull(r);
    }

    public void testManualManifest() throws Exception {
        Jar jar = new Jar("dot");
        jar.setManifest(new Manifest());
        jar.setDoNotTouchManifest();
        jar.putResource("a/b", new FileResource(new File("test/bnd.jar")));
        jar.putResource("META-INF/MANIFEST.MF", new EmbeddedResource(
                "Manifest-Version: 1\r\nX: 1\r\n\r\n".getBytes(), 0));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jar.write(bout);
        
        JarInputStream jin = new JarInputStream(new ByteArrayInputStream(bout.toByteArray()));
        Manifest m = jin.getManifest();
        assertNotNull(m);
        assertEquals("1", m.getMainAttributes().getValue("X"));
        jin.close();
    }

    public void testSimple() throws ZipException, IOException {
        File file = new File("jar/asm.jar");
        Jar jar = new Jar("asm.jar", file);
        long jarTime = jar.lastModified();
        long fileTime = file.lastModified();
        long now = System.currentTimeMillis();

        // Sanity check
        assertTrue(jarTime < fileTime);
        assertTrue(fileTime <= now);

        
        // TODO see if we can improve this test case
//        // We should use the highest modification time
//        // of the files in the JAR not the JAR (though
//        // this is a backup if time is not set in the jar)
//        assertEquals(1144412850000L, jarTime);

        // Now add the file and check that
        // the modification time has changed
        jar.putResource("asm", new FileResource(file));
        assertEquals(file.lastModified(), jar.lastModified());
    }
}
