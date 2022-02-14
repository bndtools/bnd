package biz.aQute.bnd.reporter.plugins.entries.bndproject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class CodeSnippetPluginTest {
	@InjectTemporaryDirectory
	Path tmp;
	@Test
	public void testProjectWithoutSnippet() throws Exception {
		final CodeSnippetPlugin plugin = new CodeSnippetPlugin();
		final Project p = getProject();
		p.build();

		final List<?> result = plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(null, result);
	}

	@Test
	public void testProjectWithSnippet() throws Exception {
		final CodeSnippetPlugin plugin = new CodeSnippetPlugin();
		final Project p = getProjectWithSnippet(false);
		p.build();

		final List<?> result = plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	@Test
	public void testProjectWithSnippetCustomDir() throws Exception {
		final CodeSnippetPlugin plugin = new CodeSnippetPlugin();

		final Project p = getProjectWithSnippet(true);

		final Map<String, String> prop = new HashMap<>();
		prop.put(CodeSnippetPlugin.PATH_PROPERTY, p.getBase() + "/test/exo");
		plugin.setProperties(prop);

		p.build();

		final List<?> result = plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	private Workspace getWorkspace() throws Exception {
		final File wsFile = Files.createTempDirectory(tmp, "bnd-ws")
			.toFile();

		final File cnf = Files.createDirectory(Paths.get(wsFile.getPath(), "cnf"))
			.toFile();

		final File build = new File(cnf, "build.bnd");
		build.createNewFile();

		final Workspace ws = new Workspace(wsFile);

		return ws;
	}

	private Project getProject() throws Exception {
		final Workspace ws = getWorkspace();

		final File p1 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), "project"))
			.toFile();

		final File bnd1 = new File(p1, "bnd.bnd");
		bnd1.createNewFile();

		return ws.getProject("project");
	}

	private Project getProjectWithSnippet(final boolean custom) throws Exception {
		final Project p = getProject();

		String dirName = "examples";
		if (custom) {
			dirName = "exo";
		}

		final File test = Files.createDirectory(Paths.get(p.getBase()
			.getPath(), "test"))
			.toFile();

		final File examples = Files.createDirectory(Paths.get(test.getPath(), dirName))
			.toFile();

		final File code = new File(p.getBase() + "/test/" + dirName, "Test.java");
		code.createNewFile();

		IO.write(new String(
			"package biz.aQute.bnd.reporter.codesnippet.examples.first;\n" + "\n" + "/**\n" + " * ${snippet }\n"
				+ " */\n" + "public class SingleFirst {\n" + "\n" + "  public void print() {  }\n" + "}").getBytes(),
			code);

		return p;
	}
}
