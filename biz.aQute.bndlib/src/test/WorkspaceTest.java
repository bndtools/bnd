package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;

public class WorkspaceTest extends TestCase {
    
    public void testWorkspace() throws Exception {
        Workspace ws = Workspace.getWorkspace( new File("test/w o r k s p a c e"));
        
        assertEquals( "parent", ws.getProperty("override"));
        assertEquals( "true", ws.getProperty("ext"));
        assertEquals( "abcdef", ws.getProperty("test"));
    }

    
}
