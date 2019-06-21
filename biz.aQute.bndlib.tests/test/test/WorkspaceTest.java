package test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

public class WorkspaceTest extends TestCase {

	File tmp;

	@Override
	protected void setUp() throws IOException {
		tmp = new File("generated/tmp/test/" + getName()).getAbsoluteFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);
		IO.copy(IO.getFile("testresources/ws-gestalt"), tmp);

	}

	@Override
	protected void tearDown() {
		IO.delete(tmp);
	}

	/**
	 * In an IDE the workspace must be informed if the set of projects change
	 */
	public void testProjectsWhereMacro() throws Exception {
		try (Workspace ws = Workspace.getWorkspace(IO.getFile("testresources/ws"))) {
			ws.setProperty("allprojects", "${projectswhere}");
			List<String> projects = Strings.split(ws.getProperty("allprojects"));
			assertThat(ws.check()).isTrue();
			assertThat(projects).hasSize(21);
			System.out.println(projects);

			ws.setProperty("allprojects", "${projectswhere;foo}");
			List<String> withFoo = Strings.split(ws.getProperty("allprojects"));
			assertThat(ws.check()).isTrue();
			System.out.println(withFoo);
			assertThat(withFoo).hasSize(3);

			ws.setProperty("allprojects", "${projectswhere;foo;!*}");
			List<String> withoutFoo = Strings.split(ws.getProperty("allprojects"));
			assertThat(ws.check()).isTrue();
			System.out.println(withoutFoo);
			assertThat(withoutFoo).hasSize(18);

			ws.setProperty("allprojects", "${projectswhere;foo;1|2}");
			List<String> twoProjects = Strings.split(ws.getProperty("allprojects"));
			assertThat(ws.check()).isTrue();
			System.out.println(twoProjects);
			assertThat(twoProjects).hasSize(2);

			List<String> total = new ArrayList<>(withFoo);
			total.addAll(withoutFoo);
			Collections.sort(projects);
			Collections.sort(total);
			assertThat(projects).isEqualTo(total);
		}
	}

	/**
	 * In an IDE the workspace must be informed if the set of projects change
	 */
	public void testProjectChangesEnabled() throws Exception {
		try (Workspace w = new Workspace(tmp)) {
			assertThat(getNames(w)).containsExactlyInAnyOrder("p1");

			w.createProject("newproject");
			assertThat(getNames(w)).containsExactlyInAnyOrder("p1", "newproject");

			w.refreshProjects();
			assertThat(getNames(w)).containsExactlyInAnyOrder("p1", "newproject");

			Project newproject = w.getProject("newproject");
			assertThat(newproject).isNotNull();

			newproject.remove();
			assertThat(getNames(w)).containsExactlyInAnyOrder("p1");

			w.refreshProjects();
			assertThat(getNames(w)).containsExactlyInAnyOrder("p1");
		}
	}

	private Set<String> getNames(Workspace w) throws Exception {
		return w.getAllProjects()
			.stream()
			.map(Project::getName)
			.collect(toSet());
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

	public void testGestaltGlobal() throws Exception {
		Workspace.resetStatic();
		Attrs attrs = new Attrs();
		attrs.put("x", "10");
		Workspace.addGestalt("peter", attrs);
		try (Workspace w = new Workspace(tmp)) {
			w.refresh(); // remove previous tests
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
			assertTrue(w.check());
		}
	}

	public void testGestaltLocal() throws Exception {
		Workspace.resetStatic();
		try (Workspace w = new Workspace(tmp)) {
			w.refresh(); // remove previous tests
			w.setProperty("-gestalt", "john;z=100, mieke;a=1000, ci");
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter;x}"));
			assertEquals("", w.getReplacer()
				.process("${gestalt;peter;x;10}"));
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

			assertTrue(w.check());
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
			assertNotNull(p);
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
	}

	public void testJavacDefaults() throws Exception {
		String version = System.getProperty("java.specification.version", "1.8");
		try (Workspace w = new Workspace(tmp)) {
			assertEquals(version, w.getProperty("javac.source"));
			assertEquals(version, w.getProperty("javac.target"));
		}
	}
}
