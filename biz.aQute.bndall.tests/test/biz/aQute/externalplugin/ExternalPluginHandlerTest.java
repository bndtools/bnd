package biz.aQute.externalplugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import aQute.bnd.build.Workspace;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.result.Result;
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

			Result<Object, String> call = ws.getExternalPlugins()
				.call("hellocallable", Callable.class, callable -> {
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
	public void testCallMainClass() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Result<Integer, String> call = ws.getExternalPlugins()
				.call("biz.aQute.bndall.tests.plugin_2.MainClass", null, ws, Collections.emptyMap(),
					Collections.emptyList(), null, bout, null);
			System.out.println(call);
			assertThat(call.isOk()).isTrue();
			assertThat(new String(bout.toByteArray(), "UTF-8")).contains("Hello world");
		}
	}

	@Test
	public void testNotFound() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<Object, String> call = ws.getExternalPlugins()
				.call("doesnotexist", Callable.class, callable -> Result.ok(callable.call()));
			System.out.println(call);
			assertThat(call.isOk()).isFalse();
			assertThat(call.error()
				.get()).contains("no such plugin doesnotexist for type");
		}
	}

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
