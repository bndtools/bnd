package test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class WorkspaceTest {
	@InjectTemporaryDirectory
	File testDir;

	@Test
	public void testVersionDefaultsWithBndWorkspace() throws Exception {
		IO.copy(IO.getFile("testresources/ws"), testDir);
		IO.getFile(testDir, "cnf")
			.mkdirs();
		try (Workspace ws = new Workspace(testDir)) {
			assertIsCurrent(ws);
		}


		IO.store(Constants.VERSIONDEFAULTS + " 5.2.0\n", IO.getFile(testDir, "cnf/build.bnd"));
		try (Workspace ws = new Workspace(testDir)) {
			assertIs5_2_0(ws);
		}
	}

	@Test
	public void testVersionDefaultsWithDefaultWorkspace() throws Exception {
		try (Workspace ws = Workspace.createDefaultWorkspace()) {
			assertIsCurrent(ws);
		}

		// there is no reasonable override
	}

	@Test
	public void testVersionDefaultsWithStandaloneWorkspace() throws Exception {
		Processor run = new Processor();
		run.setProperty(Constants.STANDALONE, "true");
		try (Workspace ws = Workspace.createStandaloneWorkspace(run, null)) {
			assertIsCurrent(ws);
		}

		run.setProperty(Constants.VERSIONDEFAULTS, "5.2.0");
		try (Workspace ws = Workspace.createStandaloneWorkspace(run, null)) {
			assertIs5_2_0(ws);
		}
	}

	private void assertIs5_2_0(Workspace ws) {
		assertThat(ws.getProperty("__versiondefaults__")).isEqualTo("5.2.0");
	}

	private void assertIsCurrent(Workspace ws) {
		String v = getCurrentVersionWithoutMicro();
		assertThat(ws.getProperty("__versiondefaults__")).isEqualTo(v);
	}

	private String getCurrentVersionWithoutMicro() {
		String v = new Version(About.CURRENT.getMajor(), About.CURRENT.getMinor(), 0).toString();
		return v;
	}

	/**
	 * In an IDE the workspace must be informed if the set of projects change
	 */
	@Test
	public void testProjectsWhereMacro() throws Exception {
		IO.copy(IO.getFile("testresources/ws"), testDir);
		try (Workspace ws = Workspace.getWorkspace(testDir)) {
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
	@Test
	public void testProjectChangesEnabled() throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), testDir);
		try (Workspace w = Workspace.getWorkspace(testDir)) {
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

	@Test
	public void testDriver() throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), testDir);
		try (Workspace w = Workspace.getWorkspace(testDir)) {
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

	@Test
	public void testGestaltGlobal() throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), testDir);
		Workspace.resetStatic();
		Attrs attrs = new Attrs();
		attrs.put("x", "10");
		Workspace.addGestalt("peter", attrs);
		try (Workspace w = Workspace.getWorkspace(testDir)) {
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
		} finally {
			Workspace.resetStatic();
		}
	}

	@Test
	public void testGestaltLocal() throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), testDir);
		Workspace.resetStatic();
		try (Workspace w = Workspace.getWorkspace(testDir)) {
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
		} finally {
			Workspace.resetStatic();
		}
	}

	@Test
	public void testWorkspace() throws Exception {
		File wsdir = new File(testDir, "w o r k s p a c e");
		IO.copy(new File("testresources/w o r k s p a c e"), wsdir);
		try (Workspace ws = Workspace.getWorkspace(wsdir)) {
			assertEquals("parent", ws.getProperty("override"));
			assertEquals("ParentPlugin", ws.getProperty("-plugin"));
			assertEquals("ParentPlugin,ExtPlugin", ws.getMergedParameters("-plugin")
				.toString());
			assertEquals("true", ws.getProperty("ext"));
			assertEquals("abcdef", ws.getProperty("test"));
		}
	}

	@Test
	public void testNestedWorkspace() throws Exception {
		IO.copy(new File("testresources/redirectws"), testDir);
		try (Workspace ws = Workspace.getWorkspace(new File(testDir, "wss/ws"))) {
			assertEquals("true", ws.getProperty("testcnf"));
			assertEquals("true", ws.getProperty("ext"));
		}
	}

	@Test
	public void testPropertyDefaulting() throws Exception {
		IO.copy(new File("testresources/ws-defaulting"), testDir);
		try (Workspace ws = Workspace.getWorkspace(testDir)) {
			Project p = ws.getProject("p1");
			assertNotNull(p);
			assertEquals("defaults", p.getProperty("myprop1"));
			assertEquals("workspace", p.getProperty("myprop2"));
			assertEquals("project", p.getProperty("myprop3"));
			assertEquals("src", p.mergeProperties("src"));
		}
	}

	@Test
	public void testIsValid() throws Exception {
		IO.copy(new File("testresources/ws"), testDir);
		try (Workspace ws = Workspace.getWorkspace(testDir)) {
			assertEquals(true, ws.isValid());
		}
	}

	@Test
	public void testJavacDefaults() throws Exception {
		IO.copy(new File("testresources/ws-gestalt"), testDir);
		String version = System.getProperty("java.specification.version", "1.8");
		try (Workspace w = Workspace.getWorkspace(testDir)) {
			assertEquals(version, w.getProperty("javac.source"));
			assertEquals(version, w.getProperty("javac.target"));
		}
	}
}
