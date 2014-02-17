package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;

public class WorkspaceTest extends TestCase {

	public static void testWorkspace() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/w o r k s p a c e"));

		assertEquals("parent", ws.getProperty("override"));
		assertEquals("ExtPlugin,ParentPlugin", ws.getProperty("-plugin"));
		assertEquals("true", ws.getProperty("ext"));
		assertEquals("abcdef", ws.getProperty("test"));
	}

	public static void testNestedWorkspace() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/redirectws/wss/ws"));

		assertEquals("true", ws.getProperty("testcnf"));
		assertEquals("true", ws.getProperty("ext"));
	}
	
	public static void testPropertyDefaulting() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("testresources/ws-defaulting"));
		
		Project p = ws.getProject("p1");
		assertEquals("defaults", p.getProperty("myprop1"));
		assertEquals("workspace", p.getProperty("myprop2"));
		assertEquals("project", p.getProperty("myprop3"));
	}

}
