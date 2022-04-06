package biz.aQute.externalplugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.result.Result;
import aQute.bnd.service.generate.Generator;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;

public class ExternalPluginHandlerTest {
	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void testSimple() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<Object> call = ws.getExternalPlugins()
				.call("hellocallable", Callable.class, callable -> {
					Object o = callable.call();
					if (o == null)
						return Result.err("null return");

					return Result.ok(o);
				});
			System.out.println(call);
			assertThat(call.isOk()).isTrue();
			assertThat(call.unwrap()).isEqualTo("hello, world");
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testListOfImplementations() throws Exception {
		Callable callable;
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<List<Callable>> implementations = ws.getExternalPlugins()
				.getImplementations(Callable.class, Attrs.EMPTY_ATTRS);

			assertThat(implementations.isOk()).isTrue();
			List<Callable> unwrap = implementations.unwrap();
			assertThat(unwrap).hasSize(1);

			callable = unwrap.get(0);
			assertThat(callable.call()).isEqualTo("hello, world");
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMultipleImplementations() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<List<Generator>> implementations = ws.getExternalPlugins()
				.getImplementations(Generator.class, Attrs.EMPTY_ATTRS);

			assertThat(implementations.isOk()).isTrue();
			List<Generator> unwrap = implementations.unwrap();
			assertThat(unwrap).hasSize(4);
		}
	}

	@Test
	public void testAbstractPlugin() throws Exception {
		Callable<?> plugin;
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			plugin = ws.getPlugin(Callable.class);
			assertThat(plugin).isNotNull();
			assertThat(plugin.call()).isEqualTo("hello, plugin-attrs");
		}
		assertThat(plugin.call()).isEqualTo("goodbye, plugin-attrs");
	}

	@Test
	public void testCallMainClass() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Result<Integer> call = ws.getExternalPlugins()
				.call("biz.aQute.bndall.tests.plugin_2.MainClass", null, ws, Collections.emptyMap(),
					Collections.emptyList(), null, bout, null);
			System.out.println(call);
			assertThat(call.isOk()).isTrue();
			assertThat(new String(bout.toByteArray(), StandardCharsets.UTF_8)).contains("Hello world");
		}
	}

	@Test
	public void testNotFound() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-1")) {
			getRepo(ws);

			Result<Object> call = ws.getExternalPlugins()
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
		ws.propertiesChanged();
	}

	private Workspace getWorkspace(File file) throws Exception {
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	private Workspace getWorkspace(String dir) throws Exception {
		return getWorkspace(new File(dir));
	}
}
