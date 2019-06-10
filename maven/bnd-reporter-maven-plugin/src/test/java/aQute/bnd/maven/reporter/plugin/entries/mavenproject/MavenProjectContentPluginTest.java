package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import junit.framework.TestCase;

public class MavenProjectContentPluginTest extends TestCase {

	public void testProjectNoBundle() throws Exception {
		final MavenProjectContentPlugin plugin = new MavenProjectContentPlugin();

		final List<?> result = (List<?>) plugin.extract(getProject(false), Locale.forLanguageTag("und"));

		assertNull(result);
	}

	public void testProjectWithBundle() throws Exception {
		final MavenProjectContentPlugin plugin = new MavenProjectContentPlugin();

		final List<?> result = (List<?>) plugin.extract(getProject(true), Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	private MavenProjectWrapper getProject(boolean withBundle) throws Exception {
		List<MavenProject> ps = new ArrayList<>();
		MavenProject parent = new MavenProject();

		final File tmpDir = Files.createTempDirectory("pp")
			.toFile();
		tmpDir.deleteOnExit();

		final File target = Files.createDirectories(Paths.get(tmpDir.getPath(), "target"))
			.toFile();
		target.deleteOnExit();

		final File f0 = new File(tmpDir, "pom.xml");
		f0.createNewFile();
		f0.deleteOnExit();

		if (withBundle) {
			final File jar = Files
				.copy(Paths.get("src/test/resources/bundle/simple.jar"), Paths.get(target.getPath(), "simple.jar"))
				.toFile();
			jar.deleteOnExit();
		}

		Build b = new Build();
		b.setDirectory(target.getAbsolutePath());
		b.setFinalName("simple");
		parent.getModel()
			.setBuild(b);
		parent.setPackaging("jar");
		parent.setFile(f0);

		ps.add(parent);

		return new MavenProjectWrapper(ps, parent);
	}
}
