package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class LibraryTest {
	@InjectTemporaryDirectory
	File testDir;

	@SuppressWarnings("resource")
	@Test
	public void testSimple() throws Exception {
		IO.copy(IO.getFile("testresources/ws-library"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			RepositoryPlugin repository = ws.getRepository("Local");
			assertThat(repository).isNotNull();

			Builder b = new Builder();
			b.setBundleSymbolicName("foo");
			b.setBundleVersion("0.0.1");
			b.setProperty("Provide-Capability",
				"bnd.library; bnd.library=foo; version= 1.2.3; path           = lib/foo");
			b.setIncludeResource(
				"" //
					+ "lib/foo/workspace.bnd; literal='foo=workspace\n', " //
					+ "lib/foo/project.bnd;   literal='foo=project\n', " //
					+ "lib/foo/bndrun.bnd;    literal='foo=bndrun\n', " //
					+ "lib/foo/xyz.bnd;       literal='bar=xyz\n-include ${.}/temp\n', " //
					+ "lib/foo/temp;literal='temp=1'");
			Jar jar = b.build();
			repository.put(new JarResource(jar).openInputStream(), null);
		}

		try (Workspace ws = new Workspace(testDir)) {
			ws.setProperty("-library", "foo");
			ws.propertiesChanged();
			assertThat(ws.check("Could not find ")).isTrue();
			assertThat(ws.getProperty("foo")).isEqualTo("workspace");

			Project project = ws.getProject("p1");
			assertThat(project.getProperty("foo")).isEqualTo("project");

			Run run = Run.createRun(ws, project.getFile("test.bndrun"));
			assertThat(run.getProperty("foo")).isEqualTo("bndrun");

			project.setProperty("-library", "foo;include=xyz.bnd");
			project.propertiesChanged();
			assertThat(project.getProperty("bar")).isEqualTo("xyz");
			assertThat(project.getProperty("temp")).isEqualTo("1");


			project.setProperty("-library", "${repo;foo};version=file;where=lib/foo");
			project.unsetProperty("foo");
			project.propertiesChanged();
			assertThat(project.check());
			assertThat(project.getProperty("foo")).isEqualTo("project");

		}

	}

	@SuppressWarnings("resource")
	@Test
	public void testIncludeDirAndFileLibrary() throws Exception {
		IO.copy(IO.getFile("testresources/ws-library"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			Project project = ws.getProject("p2");
			assertThat(project.getProperty("a")).isEqualTo("1");
			assertThat(project.getProperty("b")).isEqualTo("1");
			assertThat(project.getProperty("c")).isEqualTo("1");
		}
	}

	@SuppressWarnings("resource")
	@Test
	public void testMissingLibrary() throws Exception {
		IO.copy(IO.getFile("testresources/ws-library"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			ws.setProperty("-library", "foo");
			ws.clear();
			ws.propertiesChanged();
			assertThat(ws.check("No -library for foo")).isTrue();
		}
		try (Workspace ws = new Workspace(testDir)) {
			ws.setProperty("-library", "-foo");
			ws.clear();
			ws.propertiesChanged();
			assertThat(ws.check()).isTrue();
		}

	}

	@SuppressWarnings("resource")
	@Test
	public void testLatestVersion() throws Exception {
		IO.copy(IO.getFile("testresources/ws-library"), testDir);
		try (Workspace ws = new Workspace(testDir)) {
			RepositoryPlugin repository = ws.getRepository("Local");
			assertThat(repository).isNotNull();

			Builder b1 = new Builder();
			b1.setBundleSymbolicName("foo");
			b1.setBundleVersion("0.0.1");
			b1.setProperty("Provide-Capability",
				"bnd.library; bnd.library=foo; version= 1.2.3; path           = lib/foo");
			b1.setIncludeResource(
				"lib/foo/project.bnd;literal='foo=1.2.3\n',lib/foo/workspace.bnd;literal='ws=1.2.3\n'");
			Jar j1 = b1.build();
			repository.put(new JarResource(j1).openInputStream(), null);

			Builder b2 = new Builder();
			b2.setBundleSymbolicName("foo");
			b2.setBundleVersion("0.0.2");
			b2.setProperty("Provide-Capability",
				"bnd.library; bnd.library=foo; version= 1.2.4; path           = lib/foo");
			b2.setIncludeResource(
				"lib/foo/project.bnd;literal='foo=1.2.4\n',lib/foo/workspace.bnd;literal='ws=1.2.4\n'");
			Jar j2 = b2.build();
			repository.put(new JarResource(j2).openInputStream(), null);

			Builder b3 = new Builder();
			b3.setBundleSymbolicName("foo");
			b3.setBundleVersion("0.0.3");
			b3.setProperty("Provide-Capability",
				"bnd.library; bnd.library=foo; version= 1.2.2; path           = lib/foo");
			b3.setIncludeResource(
				"lib/foo/project.bnd;literal='foo=1.2.2\n',lib/foo/workspace.bnd;literal='ws=1.2.2\n'");
			Jar j3 = b3.build();
			repository.put(new JarResource(j3).openInputStream(), null);
		}

		try (Workspace ws = new Workspace(testDir)) {
			Project project = ws.getProject("p1");
			assertThat(project.getProperty("foo")).isEqualTo("1.2.4");

			project.setProperty("-library", "foo;version='[1.2.2,1.2.2]'");
			project.unsetProperty("foo");
			project.propertiesChanged();
			assertThat(project.getProperty("foo")).isEqualTo("1.2.2");

		}

		IO.store("-library=foo", new File(IO.mkdirs(new File(testDir, "cnf/ext")), "test.bnd"));
		try (Workspace ws = new Workspace(testDir)) {
			assertThat(ws.getProperty("ws")).isEqualTo("1.2.4");
			assertThat(ws.getProperty("foo")).isNull();
		}
	}
}
