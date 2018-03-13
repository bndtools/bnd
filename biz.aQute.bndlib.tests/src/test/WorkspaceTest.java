package test;

import java.io.File;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class WorkspaceTest extends TestCase {

	File tmp = new File("tmp");

	@Override
	protected void setUp() {
		IO.delete(tmp);
		tmp.mkdir();
	}

	@Override
	protected void tearDown() {
		IO.delete(tmp);
	}

	public void testDriver() throws Exception {
		try (Workspace w = new Workspace(tmp)) {
			assertEquals("unset", w.getDriver());
			assertEquals("unset", w.getReplacer()
				.process("${driver}"));
			assertEquals("unset", w.getReplacer()
				.process("${driver;unset}"));
			assertEquals("", w.getReplacer()
				.process("${driver;set}"));

			Workspace.setDriver("test");
			assertEquals("test", w.getDriver());
			assertEquals("test", w.getReplacer()
				.process("${driver}"));
			assertEquals("test", w.getReplacer()
				.process("${driver;test}"));
			assertEquals("", w.getReplacer()
				.process("${driver;nottest}"));

			w.setProperty("-bnd-driver", "test2");
			assertEquals("test2", w.getDriver());
			assertEquals("test2", w.getReplacer()
				.process("${driver}"));
			assertEquals("test2", w.getReplacer()
				.process("${driver;test2}"));
			assertEquals("", w.getReplacer()
				.process("${driver;nottest}"));
		}
	}

	public void testGestalt() throws Exception {
		Attrs attrs = new Attrs();
		attrs.put("x", "10");
		Workspace.addGestalt("peter", attrs);
		try (Workspace w = new Workspace(tmp)) {
			assertEquals("peter", w.getReplacer()
				.process("${gestalt;peter}"));
			assertEquals("10", w.getReplacer()
				.process("${gestalt;peter;x}"));
			assertEquals("10", w.getReplacer()
				.process("${gestalt;peter;x;10}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter;x;11}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter;y}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;john}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;john;x}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;john;x;10}"));
		}

		try (Workspace w = new Workspace(tmp)) {
			w.setProperty("-gestalt", "john;z=100, mieke;a=1000, ci");
			assertEquals("peter", w.getReplacer()
				.process("${gestalt;peter}"));
			assertEquals("10", w.getReplacer()
				.process("${gestalt;peter;x}"));
			assertEquals("10", w.getReplacer()
				.process("${gestalt;peter;x;10}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter;x;11}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter;y}"));
			assertEquals("john", w.getReplacer()
				.process("${gestalt;john}"));
			assertEquals("100", w.getReplacer()
				.process("${gestalt;john;z}"));
			assertEquals("100", w.getReplacer()
				.process("${gestalt;john;z;100}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;john;z;101}"));
			assertEquals("mieke", w.getReplacer()
				.process("${gestalt;mieke}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;mieke;x}"));
		}
	}

	public void testWorkspace() throws Exception {
		try (Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/w o r k s p a c e"))) {
			assertEquals("parent", ws.getProperty("override"));
			assertEquals("ExtPlugin,ParentPlugin", ws.getProperty("-plugin"));
			assertEquals("true", ws.getProperty("ext"));
			assertEquals("abcdef", ws.getProperty("test"));
		}
	}

	public void testNestedWorkspace() throws Exception {
		try (Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/redirectws/wss/ws"))) {
			assertEquals("true", ws.getProperty("testcnf"));
			assertEquals("true", ws.getProperty("ext"));
		}
	}

	public void testPropertyDefaulting() throws Exception {
		try (Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/ws-defaulting"))) {
			Project p = ws.getProject("p1");
			assertEquals("defaults", p.getProperty("myprop1"));
			assertEquals("workspace", p.getProperty("myprop2"));
			assertEquals("project", p.getProperty("myprop3"));
			assertEquals("src", p.mergeProperties("src"));
		}
	}

	public void testIsValid() throws Exception {
		try (Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/ws"))) {
			assertEquals(true, ws.isValid());
		}

		try (Workspace invalidWs = new Workspace(IO.getFile("testresources/not a workspace"))) {
			assertEquals(false, invalidWs.isValid());
		}
	}

	public void testJavacDefaults() throws Exception {
		String version = System.getProperty("java.specification.version", "1.8");
		try (Workspace w = new Workspace(tmp)) {
			assertEquals(version, w.getProperty("javac.source"));
			assertEquals(version, w.getProperty("javac.target"));
		}
	}
}
