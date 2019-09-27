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
import junit.framework.TestCase;

public class MavenAggregatorConcentPluginTest extends TestCase {

	public void testEmptyWorkspace() throws Exception {
		final MavenAggregatorConcentPlugin plugin = new MavenAggregatorConcentPlugin();

		assertNull(plugin.extract(getProject(), Locale.forLanguageTag("und")));
	}

	public void testWorkspaceWithProjects() throws Exception {
		final MavenAggregatorConcentPlugin plugin = new MavenAggregatorConcentPlugin();

		final List<?> result = (List<?>) plugin.extract(getProjects("test1", "test2"), Locale.forLanguageTag("und"));

		assertEquals(2, result.size());
	}

	public void testWorkspaceWithProjectsFilter() throws Exception {
		final MavenAggregatorConcentPlugin plugin = new MavenAggregatorConcentPlugin();

		final Map<String, String> prop = new HashMap<>();
		prop.put(MavenAggregatorConcentPlugin.EXCLUDES_PROPERTY, "test1");
		plugin.setProperties(prop);

		final List<?> result = (List<?>) plugin.extract(getProjects("test1", "test2"), Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	private MavenProjectWrapper getProject() throws Exception {
		List<MavenProject> ps = new ArrayList<>();
		MavenProject p = new MavenProject();

		ps.add(p);

		return new MavenProjectWrapper(ps, p);
	}

	private MavenProjectWrapper getProjects(String p1, String p2) throws Exception {
		List<MavenProject> ps = new ArrayList<>();
		MavenProject parent = new MavenProject();
		MavenProject first = new MavenProject();
		MavenProject second = new MavenProject();

		final File tmpDir = Files.createTempDirectory("pp")
			.toFile();
		tmpDir.deleteOnExit();

		final File d1 = Files.createDirectories(Paths.get(tmpDir.getPath(), p1))
			.toFile();
		d1.deleteOnExit();
		final File d2 = Files.createDirectories(Paths.get(tmpDir.getPath(), p2))
			.toFile();
		d2.deleteOnExit();

		final File f0 = new File(tmpDir, "pom.xml");
		f0.createNewFile();
		f0.deleteOnExit();

		final File f1 = new File(d1, "pom.xml");
		f1.createNewFile();
		f1.deleteOnExit();

		final File f2 = new File(d2, "pom.xml");
		f2.createNewFile();
		f2.deleteOnExit();

		parent.setFile(f0);
		first.setFile(f1);
		second.setFile(f2);

		parent.getModel()
			.addModule(p1);
		parent.getModel()
			.addModule(p2);

		ps.add(parent);
		ps.add(first);
		ps.add(second);

		return new MavenProjectWrapper(ps, parent);
	}
}
