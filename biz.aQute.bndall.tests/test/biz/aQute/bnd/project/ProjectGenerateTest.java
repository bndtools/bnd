package biz.aQute.bnd.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.result.Result;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;

public class ProjectGenerateTest {
	@Rule
	public TemporaryFolder	tf	= new TemporaryFolder();
	File					tmp;


	@Before
	public void setUp() throws Exception {
		tmp = tf.newFolder();
	}

	/**
	 * Test -stalecheck
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaleChecks() throws Exception {

		Workspace ws = getWorkspace("resources/ws-stalecheck");
		Project project = ws.getProject("p1");
		File foobar = project.getFile("foo.bar");

		foobar.setLastModified(System.currentTimeMillis() + 100_000);
		File f = testStaleCheck(project, "\"foo.bar,bnd.bnd\";newer=\"older\";error=FOO", "FOO");
		assertThat(f).isNull();

		f = testStaleCheck(project, "'foo.bar,bnd.bnd';newer=\"older/,younger/\"",
			"detected stale files ");
		assertThat(f).isNotNull();

		f = testStaleCheck(project, "foo.bar;newer=older/;warning=FOO", "FOO");
		assertThat(f).isNotNull();

		if (!IO.isWindows()) {
			f = testStaleCheck(project, "foo.bar;newer=older;command='cp foo.bar older/'");
			try (Jar t = new Jar(f)) {
				assertThat(t.getResource("b/c/foo.txt")).isNotNull();
				assertThat(t.getResource("foo.bar")).isNotNull();
			}
		}

		foobar.setLastModified(0);
		testStaleCheck(project, "foo.bar;newer=older");
		testStaleCheck(project, "foo.bar;newer=older;error=FOO");
		testStaleCheck(project, "foo.bar;newer=older;warning=FOO");

		testStaleCheck(project, "older/;newer=foo.bar", "detected");
		testStaleCheck(project, "older/;newer=foo.bar;error=FOO", "FOO");
		testStaleCheck(project, "older/;newer=foo.bar;warning=FOO", "FOO");
	}

	File testStaleCheck(Project project, String clauses, String... check) throws Exception {
		project.clean();
		project.setProperty("-resourcesonly", "true");
		project.setProperty("-includeresource", "older");
		project.setProperty("-stalecheck", clauses);
		File[] build = project.build();
		assertThat(project.check(check)).isTrue();
		return build != null ? build[0] : null;
	}

	@Test
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void testSimpleGenerator() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-stalecheck")) {
			getRepo(ws);
			Project project = ws.getProject("p2");
			project.setProperty("-generate",
				"gen/**.java;output=src-gen/;generate='javagen -o src-gen/ gen/**.java'");

			File out = project.getFile("src-gen/foo/bar/Buildpath.java");
			assertThat(out).doesNotExist();
			project.getGenerate()
				.generate(true);
			assertThat(project.check()).isTrue();
			assertThat(out).isFile();
		}
	}

	@Test
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void testThrowExceptionn() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-stalecheck")) {
			getRepo(ws);
			Project project = ws.getProject("p2");

			project.setProperty("-generate", "gen/*;output=gen-src/;generate=throwexception");
			Result<Set<File>, String> result = project.getGenerate()
				.generate(true);
			assertThat(project.check("'throwexception' failed with")).isTrue();
		}
	}

	@Test
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void testEmptyHeader() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-stalecheck")) {
			getRepo(ws);
			Project project = ws.getProject("p2");

			project.setProperty("-generate", "<<EMPTY>>;output=gen-src/");
			Result<Set<File>, String> result = project.getGenerate()
				.generate(true);
			assertThat(result.error()).isNotPresent();
			assertThat(result.unwrap()).isEmpty();
			assertThat(project.check()).isTrue();
		}
	}

	@Test
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void testPluginWithError() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-stalecheck")) {
			getRepo(ws);
			Project project = ws.getProject("p2");

			project.setProperty("-generate", "gen/**.java;output=gen-src/;generate='error'");
			Result<Set<File>, String> result = project.getGenerate()
				.generate(true);
			assertThat(result.error()
				.get()).contains("error");
			assertThat(project.check("error : gen/")).isTrue();
		}
	}


	private Workspace getWorkspace(File file) throws Exception {
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	private Workspace getWorkspace(String dir) throws Exception {
		return getWorkspace(new File(dir));
	}

	private void getRepo(Workspace ws) throws IOException, Exception {
		System.out.println("current working dir " + IO.work);
		FileTree tree = new FileTree();
		List<File> files = tree.getFiles(IO.getFile("generated/"), "*.jar");
		File file = IO.getFile("../biz.aQute.bnd.javagen/generated/")
			.getCanonicalFile();
		System.out.println("where " + file);
		files.addAll(tree.getFiles(file, "*.jar"));
		System.out.println("tmp repo " + files);
		FileSetRepository repo = new FileSetRepository("test", files);
		ws.addBasicPlugin(repo);
	}
}
