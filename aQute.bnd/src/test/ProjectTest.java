package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.lib.deployer.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.eclipse.*;
import aQute.libg.version.*;

public class ProjectTest extends TestCase {
    
    public void testOutofDate() throws Exception {
        Workspace ws = Workspace.getWorkspace(new File("test/ws"));
        Project project = ws.getProject("p3");
        File bnd = new File("test/ws/p3/bnd.bnd");
        assertTrue( bnd.exists());
        
        project.clean();
        project.getTarget().mkdirs();

        // Now we build it.
        File [] files = project.build();
        assertNotNull(files);
        assertEquals(1, files.length);

        // Now we should not rebuild it
        long lastTime = files[0].lastModified();
        files = project.build();
        assertEquals(1, files.length);
        assertTrue ( files[0].lastModified() == lastTime);
        
        Thread.sleep(2000);
        
        project.updateModified(System.currentTimeMillis(), "Testing");
        files = project.build();
        assertEquals(1, files.length);
        assertTrue ("Must have newer files now", files[0].lastModified() > lastTime);
    }

    public void testRepoMacro() throws Exception {
        Workspace ws = Workspace.getWorkspace(new File("test/ws"));
        Project project = ws.getProject("p2");
        System.out.println(project.getPlugins(FileRepo.class));
        String s = project.getReplacer().process(("${repo;libtest}"));
        System.out.println( s );
        assertTrue( s.contains("org.apache.felix.configadmin/org.apache.felix.configadmin-1.1.0"));
        assertTrue( s.contains("org.apache.felix.ipojo/org.apache.felix.ipojo-1.0.0.jar"));
    }
    
	public void testClasspath() throws Exception {
		File	project = new File("").getAbsoluteFile();
		File workspace = project.getParentFile();
		Processor processor = new Processor();
		EclipseClasspath p = new EclipseClasspath( processor, workspace, project );
		System.out.println( p.getDependents());
		System.out.println( p.getClasspath());
		System.out.println( p.getSourcepath());
		System.out.println( p.getOutput());
	}
	
	public void testBump() throws Exception {
	    Workspace ws = Workspace.getWorkspace(new File("test/ws"));
	    Project project = ws.getProject("p1");
	    int size = project.getProperties().size();
	    Version old = new Version(project.getProperty("Bundle-Version"));
	    project.bump("=+0");
	    Version newv = new Version(project.getProperty("Bundle-Version"));
        assertEquals( old.getMajor(), newv.getMajor());
        assertEquals( old.getMinor()+1, newv.getMinor());
        assertEquals( 0, newv.getMicro());
        assertEquals( size, project.getProperties().size());
        assertEquals( "sometime", newv.getQualifier());
	}
	
	
	
	
}
