package biz.aQute.resolve;

import java.io.File;
import java.util.List;

import org.osgi.service.repository.Repository;

import aQute.bnd.build.Run;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class StandaloneTest extends TestCase {

	public void testStandalone() throws Exception {
		File f = IO.getFile("testdata/standalone/simple.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("foo", f0.getName());
		assertEquals("http://example.org/index.xml", f0.getLocation());
	}

	public void testMultipleUrls() throws Exception {
		File f = IO.getFile("testdata/standalone/multi.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);
		assertTrue(repositories.get(1) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("repo01", f0.getName());
		assertEquals("http://example.org/index1.xml", f0.getLocation());

		OSGiRepository f1 = (OSGiRepository) repositories.get(1);
		assertEquals("second", f1.getName());
		assertEquals("http://example.org/index2.xml", f1.getLocation());
	}

	public void testRelativeUrl() throws Exception {
		File f = IO.getFile("testdata/standalone/relative_url.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);
		assertTrue(repositories.get(1) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("repo01", f0.getName());
		String resolvedUrl = IO.getFile("testdata/larger-repo.xml")
			.toURI()
			.toString();
		assertEquals(resolvedUrl, f0.getLocation());

		OSGiRepository f1 = (OSGiRepository) repositories.get(1);
		assertEquals("repo02", f1.getName());

		assertEquals("http://example.org/index2.xml", f1.getLocation());
	}

	public void testExtraAttribs() throws Exception {
		File f = IO.getFile("testdata/standalone/attribs.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("foo", f0.getName());
		assertEquals("http://example.org/index.xml", f0.getLocation());

		File cacheDir = IO.getFile(System.getProperty("user.home") + "/.custom_cache_dir");
		assertEquals(cacheDir, f0.getRoot());
	}

	public void testMacroExpansion() throws Exception {
		File f = IO.getFile("testdata/standalone/macro.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals(System.getProperty("user.name") + " M2", f0.getName());
		File indexFile = IO.getFile(System.getProperty("user.home") + "/.m2/repository/repository.xml");
		assertEquals(indexFile.toURI()
			.toString(), f0.getLocation());
	}
}
