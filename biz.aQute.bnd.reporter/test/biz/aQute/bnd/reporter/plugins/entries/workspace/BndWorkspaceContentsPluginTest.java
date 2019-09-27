package biz.aQute.bnd.reporter.plugins.entries.workspace;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.build.Workspace;
import biz.aQute.bnd.reporter.plugins.entries.bndworkspace.BndWorkspaceContentsPlugin;
import junit.framework.TestCase;

public class BndWorkspaceContentsPluginTest extends TestCase {

	public void testEmptyWorkspace() throws Exception {
		final BndWorkspaceContentsPlugin plugin = new BndWorkspaceContentsPlugin();

		assertNull(plugin.extract(getWorkspace(), Locale.forLanguageTag("und")));
	}

	public void testWorkspaceWithProjects() throws Exception {
		final BndWorkspaceContentsPlugin plugin = new BndWorkspaceContentsPlugin();

		final List<?> result = (List<?>) plugin.extract(getWorkspace("test1", "test2"), Locale.forLanguageTag("und"));

		assertEquals(2, result.size());
	}

	public void testWorkspaceWithProjectsFilter() throws Exception {
		final BndWorkspaceContentsPlugin plugin = new BndWorkspaceContentsPlugin();

		final Map<String, String> prop = new HashMap<>();
		prop.put(BndWorkspaceContentsPlugin.EXCLUDES_PROPERTY, "test1");
		plugin.setProperties(prop);

		final List<?> result = (List<?>) plugin.extract(getWorkspace("test1", "test2"), Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	private Workspace getWorkspace() throws Exception {
		final File wsFile = Files.createTempDirectory("bnd-ws")
			.toFile();
		wsFile.deleteOnExit();

		final File cnf = Files.createDirectory(Paths.get(wsFile.getPath(), "cnf"))
			.toFile();
		cnf.deleteOnExit();

		final File build = new File(cnf, "build.bnd");
		build.createNewFile();
		build.deleteOnExit();

		final Workspace ws = new Workspace(wsFile);

		return ws;
	}

	private Workspace getWorkspace(final String project1, final String project2) throws Exception {
		final Workspace ws = getWorkspace();

		final File p1 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), project1))
			.toFile();
		p1.deleteOnExit();

		final File bnd1 = new File(p1, "bnd.bnd");
		bnd1.createNewFile();
		bnd1.deleteOnExit();

		final File p2 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), project2))
			.toFile();
		p2.deleteOnExit();

		final File bnd2 = new File(p2, "bnd.bnd");
		bnd2.createNewFile();
		bnd2.deleteOnExit();

		return ws;
	}
}
