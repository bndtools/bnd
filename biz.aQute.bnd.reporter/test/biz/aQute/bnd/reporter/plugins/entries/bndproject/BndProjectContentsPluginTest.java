package biz.aQute.bnd.reporter.plugins.entries.bndproject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import junit.framework.TestCase;

public class BndProjectContentsPluginTest extends TestCase {

	public void testProjectSingleContents() throws Exception {
		final BndProjectContentsPlugin plugin = new BndProjectContentsPlugin();
		final Project p = getProject();
		p.build();
		final List<?> result = (List<?>) plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(1, result.size());
	}

	public void testProjectMultiContents() throws Exception {
		final BndProjectContentsPlugin plugin = new BndProjectContentsPlugin();
		final Project p = getProjectWithSub();
		p.build();
		final List<?> result = (List<?>) plugin.extract(p, Locale.forLanguageTag("und"));

		assertEquals(2, result.size());
	}

	public void testProjectMultiContentsWithFilter() throws Exception {
		final BndProjectContentsPlugin plugin = new BndProjectContentsPlugin();

		final Map<String, String> prop = new HashMap<>();
		prop.put(BndProjectContentsPlugin.EXCLUDES_PROPERTY, "project.a");
		plugin.setProperties(prop);

		final Project p = getProjectWithSub();
		p.build();
		final List<?> result = (List<?>) plugin.extract(p, Locale.forLanguageTag("und"));

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

	private Project getProject() throws Exception {
		final Workspace ws = getWorkspace();

		final File p1 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), "project"))
			.toFile();
		p1.deleteOnExit();

		final File bnd1 = new File(p1, "bnd.bnd");
		bnd1.createNewFile();
		bnd1.deleteOnExit();

		return ws.getProject("project");
	}

	private Project getProjectWithSub() throws Exception {
		final Project p = getProject();

		final File bnd1 = new File(p.getBase(), "a.bnd");
		bnd1.createNewFile();
		bnd1.deleteOnExit();

		final File bnd2 = new File(p.getBase(), "b.bnd");
		bnd2.createNewFile();
		bnd2.deleteOnExit();

		p.setProperty("-sub", "a.bnd,b.bnd");
		return p;
	}
}
