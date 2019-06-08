package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.project.MavenProject;

import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class CodeSnippetPluginTest extends TestCase {

	public void testProjectWithoutSnippet() throws Exception {
		final CodeSnippetPlugin plugin = new CodeSnippetPlugin();
		final MavenProjectWrapper p = getProject();

		final List<?> result = plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(null, result);
	}

	public void testProjectWithSnippet() throws Exception {
		final CodeSnippetPlugin plugin = new CodeSnippetPlugin();
		final MavenProjectWrapper p = getProjectWithSnippet(false);

		final List<?> result = plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	public void testProjectWithSnippetCustomDir() throws Exception {
		final CodeSnippetPlugin plugin = new CodeSnippetPlugin();

		final MavenProjectWrapper p = getProjectWithSnippet(true);

		final Map<String, String> prop = new HashMap<>();
		prop.put(CodeSnippetPlugin.PATH_PROPERTY, p.getProject()
			.getBasedir()
			.getAbsolutePath() + "/test/exo");
		plugin.setProperties(prop);

		final List<?> result = plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	private MavenProjectWrapper getProject() throws Exception {
		List<MavenProject> ps = new ArrayList<>();
		MavenProject parent = new MavenProject();

		final File tmpDir = Files.createTempDirectory("pp")
			.toFile();
		tmpDir.deleteOnExit();

		final File sourceTest = Files.createDirectories(Paths.get(tmpDir.getPath(), "test"))
			.toFile();
		sourceTest.deleteOnExit();

		final File f0 = new File(tmpDir, "pom.xml");
		f0.createNewFile();
		f0.deleteOnExit();

		parent.setFile(f0);
		parent.addTestCompileSourceRoot(sourceTest.getAbsolutePath());

		ps.add(parent);

		return new MavenProjectWrapper(ps, parent);
	}

	private MavenProjectWrapper getProjectWithSnippet(final boolean custom) throws Exception {
		final MavenProjectWrapper p = getProject();

		String dirName = "examples";
		if (custom) {
			dirName = "exo";
		}

		final File test = new File(p.getProject()
			.getTestCompileSourceRoots()
			.get(0));

		final File examples = Files.createDirectory(Paths.get(test.getPath(), dirName))
			.toFile();
		examples.deleteOnExit();

		final File code = new File(p.getProject()
			.getTestCompileSourceRoots()
			.get(0) + "/" + dirName, "Test.java");
		code.createNewFile();

		IO.write(new String(
			"package biz.aQute.bnd.reporter.codesnippet.examples.first;\n" + "\n" + "/**\n" + " * ${snippet }\n"
				+ " */\n" + "public class SingleFirst {\n" + "\n" + "  public void print() {  }\n" + "}").getBytes(),
			code);

		code.deleteOnExit();

		return p;
	}
}
