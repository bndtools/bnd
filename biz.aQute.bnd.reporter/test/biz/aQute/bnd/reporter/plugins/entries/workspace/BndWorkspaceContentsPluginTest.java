package biz.aQute.bnd.reporter.plugins.entries.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import biz.aQute.bnd.reporter.plugins.entries.bndworkspace.BndWorkspaceContentsPlugin;

public class BndWorkspaceContentsPluginTest {
	@InjectTemporaryDirectory
	Path tmp;
	@Test
	public void testEmptyWorkspace() throws Exception {
		final BndWorkspaceContentsPlugin plugin = new BndWorkspaceContentsPlugin();

		assertNull(plugin.extract(getWorkspace(), Locale.forLanguageTag("und")));
	}

	@Test
	public void testWorkspaceWithProjects() throws Exception {
		final BndWorkspaceContentsPlugin plugin = new BndWorkspaceContentsPlugin();

		final List<?> result = (List<?>) plugin.extract(getWorkspace("test1", "test2"), Locale.forLanguageTag("und"));

		assertEquals(2, result.size());
	}

	@Test
	public void testWorkspaceWithProjectsFilter() throws Exception {
		final BndWorkspaceContentsPlugin plugin = new BndWorkspaceContentsPlugin();

		final Map<String, String> prop = new HashMap<>();
		prop.put(BndWorkspaceContentsPlugin.EXCLUDES_PROPERTY, "test1");
		plugin.setProperties(prop);

		final List<?> result = (List<?>) plugin.extract(getWorkspace("test1", "test2"), Locale.forLanguageTag("und"));

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

	private Workspace getWorkspace(final String project1, final String project2) throws Exception {
		final Workspace ws = getWorkspace();

		final File p1 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), project1))
			.toFile();

		final File bnd1 = new File(p1, "bnd.bnd");
		bnd1.createNewFile();

		final File p2 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), project2))
			.toFile();

		final File bnd2 = new File(p2, "bnd.bnd");
		bnd2.createNewFile();
		ws.refreshProjects();
		return ws;
	}
}
