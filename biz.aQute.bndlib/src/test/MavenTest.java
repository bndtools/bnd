package test;

import java.io.*;
import java.net.*;

import junit.framework.*;
import aQute.bnd.maven.*;
import aQute.lib.osgi.*;

public class MavenTest extends TestCase {
    Processor processor = new Processor();
    
    
    
    public void testDependencies() throws Exception {
    	MavenDependencyGraph	graph;
    	
    	graph = new MavenDependencyGraph();
    	File home = new File( System.getProperty("user.home"));
    	File m2 = new File( home, ".m2");
    	File m2Repo = new File( m2, "repository");
    	if ( m2Repo.isDirectory())
    		graph.addRepository( m2Repo.toURI().toURL());
    	
    	graph.addRepository( new URL("http://repo1.maven.org/maven2/"));
    	graph.addRepository( new URL("http://repository.springsource.com/maven/bundles/external"));
    //	graph.root.add( new File("test/poms/pom-1.xml").toURI().toURL());
    	
    }
    
    
    
    
//    public void testMaven() {
//        MavenRepository maven = new MavenRepository();
//        maven.setReporter(processor);
//        maven.setProperties( new HashMap<String,String>());
//        List<String> x = maven.list(null);
//        System.out.println(x);
//    }
//
//
//    public void testMavenBsnMapping() throws Exception {
//        Processor processor = new Processor();
//        processor.setProperty("-plugin", "aQute.bnd.maven.MavenGroup; groupId=org.apache.felix, aQute.bnd.maven.MavenRepository");
//        MavenRepository maven = new MavenRepository();
//        maven.setReporter(processor);
//        Map<String,String> map = new HashMap<String,String>();
//        map.put("root", "test/maven-repo");
//        maven.setProperties(map);
//
//        File    files[]= maven.get("org.apache.felix.framework", null);
//        assertNotNull(files);;
//        assertEquals(1, files.length);
//
//        files = maven.get("biz.aQute.bndlib", null);
//        assertNotNull(files);;
//        assertEquals(5, files.length);
//   }
}
