package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.lib.io.*;

public class WorkspaceTest extends TestCase {

	File tmp = new File("tmp");

	public void setUp() {
		IO.delete(tmp);
		tmp.mkdir();
	}

	public void testDriver() throws Exception {
		Attrs attrs = new Attrs();
		attrs.put("x", "10");

		Workspace w = new Workspace(tmp);

		assertEquals("unset", w.getDriver());
		assertEquals("unset", w.getReplacer().process("${driver}"));
		assertEquals("unset", w.getReplacer().process("${driver;unset}"));
		assertEquals("", w.getReplacer().process("${driver;set}"));

		Workspace.setDriver("test");
		assertEquals("test", w.getDriver());
		assertEquals("test", w.getReplacer().process("${driver}"));
		assertEquals("test", w.getReplacer().process("${driver;test}"));
		assertEquals("", w.getReplacer().process("${driver;nottest}"));

		w.setProperty("-bnd-driver", "test2");
		assertEquals("test2", w.getDriver());
		assertEquals("test2", w.getReplacer().process("${driver}"));
		assertEquals("test2", w.getReplacer().process("${driver;test2}"));
		assertEquals("", w.getReplacer().process("${driver;nottest}"));

		w.close();
	}

	public void testGestalt() throws Exception {
		Attrs attrs = new Attrs();
		attrs.put("x", "10");
		Workspace.addGestalt("peter", attrs);
		Workspace w = new Workspace(tmp);

		assertEquals("peter", w.getReplacer().process("${gestalt;peter}"));
		assertEquals("10", w.getReplacer().process("${gestalt;peter;x}"));
		assertEquals("10", w.getReplacer().process("${gestalt;peter;x;10}"));
		assertEquals("", w.getReplacer().process("${gestalt;peter;x;11}"));
		assertEquals("", w.getReplacer().process("${gestalt;peter;y}"));
		assertEquals("", w.getReplacer().process("${gestalt;john}"));
		assertEquals("", w.getReplacer().process("${gestalt;john;x}"));
		assertEquals("", w.getReplacer().process("${gestalt;john;x;10}"));

		w.close();
		w = new Workspace(tmp);
		w.setProperty("-gestalt", "john;z=100, mieke;a=1000, ci");
		assertEquals("peter", w.getReplacer().process("${gestalt;peter}"));
		assertEquals("10", w.getReplacer().process("${gestalt;peter;x}"));
		assertEquals("10", w.getReplacer().process("${gestalt;peter;x;10}"));
		assertEquals("", w.getReplacer().process("${gestalt;peter;x;11}"));
		assertEquals("", w.getReplacer().process("${gestalt;peter;y}"));
		assertEquals("john", w.getReplacer().process("${gestalt;john}"));
		assertEquals("100", w.getReplacer().process("${gestalt;john;z}"));
		assertEquals("100", w.getReplacer().process("${gestalt;john;z;100}"));
		assertEquals("", w.getReplacer().process("${gestalt;john;z;101}"));
		assertEquals("mieke", w.getReplacer().process("${gestalt;mieke}"));
		assertEquals("", w.getReplacer().process("${gestalt;mieke;x}"));

		w.close();
	}

	public static void testWorkspace() throws Exception {
		Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/w o r k s p a c e"));

		assertEquals("parent", ws.getProperty("override"));
		assertEquals("ExtPlugin,ParentPlugin", ws.getProperty("-plugin"));
		assertEquals("true", ws.getProperty("ext"));
		assertEquals("abcdef", ws.getProperty("test"));
	}

	public static void testNestedWorkspace() throws Exception {
		Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/redirectws/wss/ws"));

		assertEquals("true", ws.getProperty("testcnf"));
		assertEquals("true", ws.getProperty("ext"));
	}

	public static void testPropertyDefaulting() throws Exception {
		Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/ws-defaulting"));

		Project p = ws.getProject("p1");
		assertEquals("defaults", p.getProperty("myprop1"));
		assertEquals("workspace", p.getProperty("myprop2"));
		assertEquals("project", p.getProperty("myprop3"));
		assertEquals("src", p.mergeProperties("src"));
	}

	public static void testIsValid() throws Exception {
		Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/ws"));
		assertEquals(true, ws.isValid());

		Workspace invalidWs = new Workspace(IO.getFile("testresource/not a workspace"));
		try {
			assertEquals(false, invalidWs.isValid());
		}
		finally {
			invalidWs.close();
		}
	}
}
