package biz.aQute.externalplugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import aQute.bnd.build.Workspace;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.result.Result;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;

public class ExternalPluginHandlerTest {


	@Rule
	public TemporaryFolder	tf	= new TemporaryFolder();
	File					tmp;

	public ExternalPluginHandlerTest() throws Exception {}


	@Test
	public void testSimple() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<Object, String> call = ws.call("hellocallable", Callable.class, callable -> {
				Object o = callable.call();
				if (o == null)
					return Result.err("null return");

				return Result.ok(o);
			});
			System.out.println(call);
			assertThat(call.isOk()).isTrue();
			assertThat(call.unwrap()).isEqualTo("hello");
		}
	}

	@Test
	public void testNotFound() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<Object, String> call = ws.call("doesnotexist", Callable.class, callable -> {
				return Result.ok(callable.call());
			});
			System.out.println(call);
			assertThat(call.isOk()).isFalse();
			assertThat(call.error()
				.get()).contains("no such plugin doesnotexist for type");
		}
	}

	// @Test
//	@SuppressWarnings({
//		"unchecked", "rawtypes"
//	})
//	public void testSimpleGenerator() throws Exception {
//		try (Workspace ws = getWorkspace("resources/ws-1")) {
//			getRepo(ws);
//
//			Result<Boolean, String> result = ws.call("emptyset", Generator.class, (Generator p) -> {
//				String r = p.generate(null, null);
//				if (r == null)
//					return Result.ok(true);
//				else
//					return Result.err(r);
//			});
//			assertThat(result.isOk()).isTrue();
//		}
//	}
//
//	@Test
//	public void testCallable() throws Exception {
//		try (Workspace ws = getWorkspace("resources/ws-1")) {
//			getRepo(ws);
//
//			Result<Boolean, String> result = ws.call("emptyset", Generator.class, (Generator p) -> {
//				String r = p.generate(null, null);
//				if (r == null)
//					return Result.ok(true);
//				else
//					return Result.err(r);
//			});
//			assertThat(result.isOk()).isTrue();
//		}
//	}
//
//
//	@Test
//	public void testCopyNonExistent() throws Exception {
//		try (Workspace ws = getWorkspace("resources/ws-1")) {
//			getRepo(ws);
//			Project p1 = ws.getProject("p1");
//
//			p1.setProperty("-stalecheck",
//				"t1/foo.source; newer=t1/foo.source.target; before=compile;generate=testcopy");
//			File source = p1.getFile("t1/foo.source");
//			File target = p1.getFile("t1/foo.source.target");
//			assertThat(target).doesNotExist();
//			Result<Set<File>, String> result = p1.beforeCompile(false);
//			System.out.println(result);
//			assertThat(result.unwrap()).contains(target);
//			assertThat(target.lastModified()).isGreaterThanOrEqualTo(source.lastModified());
//
//			assertThat(result.isOk()).isTrue();
//			assertThat(result.unwrap()).isNotEmpty();
//
//			// try again, should have no files changed
//			result = p1.beforeCompile(false);
//			assertThat(result.unwrap()).isEmpty();
//
//		}
//	}
//
//	@Test
//	public void testNoPlugin() throws Exception {
//		try (Workspace ws = getWorkspace("resources/ws-1")) {
//			getRepo(ws);
//			Project p1 = ws.getProject("p1");
//
//			p1.setProperty("-stalecheck",
//				"t1/foo.source; newer=t1/foo.source.target; before=compile;generate=notexistent");
//			File source = p1.getFile("t1/foo.source");
//			File target = p1.getFile("t1/foo.source.target");
//			assertThat(target).doesNotExist();
//			Result<Set<File>, String> result = p1.beforeCompile(false);
//			System.out.println(result);
//			assertThat(result.error())
//				.contains("no such plugin notexistent for type aQute.bnd.service.generate.Generator");
//
//		}
//	}
//
//	@Test
//	public void testOutputDir() throws Exception {
//		try (Workspace ws = getWorkspace("resources/ws-1")) {
//			getRepo(ws);
//			Project p1 = ws.getProject("p1");
//
//			p1.setProperty("-stalecheck", "t2/foo.source; newer=t2/out/; before=compile;generate=testcopytodir");
//			File source = p1.getFile("t2/foo.source");
//			File target = p1.getFile("t2/out/b.txt");
//			assertThat(target).doesNotExist();
//			Result<Set<File>, String> result = p1.beforeCompile(false);
//			System.out.println(result);
//			assertThat(result.unwrap()).contains(target)
//				.hasSize(3);
//			assertThat(target.lastModified()).isGreaterThanOrEqualTo(source.lastModified());
//
//			assertThat(result.isOk()).isTrue();
//			assertThat(result.unwrap()).isNotEmpty();
//		}
//	}
//
	private void getRepo(Workspace ws) throws IOException, Exception {
		FileTree tree = new FileTree();
		List<File> files = tree.getFiles(IO.getFile("generated"), "*.jar");
		FileSetRepository repo = new FileSetRepository("test", files);
		ws.addBasicPlugin(repo);
	}

	private Workspace getWorkspace(File file) throws Exception {
		tmp = tf.newFolder();
		tmp.mkdirs();
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	private Workspace getWorkspace(String dir) throws Exception {
		return getWorkspace(new File(dir));
	}
}
