package biz.aQute.resolve;

import java.io.File;
import java.util.List;

import org.osgi.service.repository.Repository;

import aQute.bnd.build.Run;
import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class StandaloneTest extends TestCase {

	public void testStandalone() throws Exception {
		File f = IO.getFile("testdata/standalone/simple.bndrun");
		Run run = Run.createStandaloneRun(f);

		List<Repository> repositories = run.getWorkspace().getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof FixedIndexedRepo);

		FixedIndexedRepo f0 = (FixedIndexedRepo) repositories.get(0);
		assertEquals("foo", f0.getName());
		assertEquals("http://example.org/index.xml", f0.getIndexLocations().get(0).toString());
	}

	public void testMultipleUrls() throws Exception {
		File f = IO.getFile("testdata/standalone/multi.bndrun");
		Run run = Run.createStandaloneRun(f);

		List<Repository> repositories = run.getWorkspace().getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof FixedIndexedRepo);
		assertTrue(repositories.get(1) instanceof FixedIndexedRepo);

		FixedIndexedRepo f0 = (FixedIndexedRepo) repositories.get(0);
		assertEquals("_1", f0.getName());
		assertEquals("http://example.org/index1.xml", f0.getIndexLocations().get(0).toString());

		FixedIndexedRepo f1 = (FixedIndexedRepo) repositories.get(1);
		assertEquals("second", f1.getName());
		assertEquals("http://example.org/index2.xml", f1.getIndexLocations().get(0).toString());
	}

	public void testRelativeUrl() throws Exception {
		File f = IO.getFile("testdata/standalone/relative_url.bndrun");
		Run run = Run.createStandaloneRun(f);

		List<Repository> repositories = run.getWorkspace().getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof FixedIndexedRepo);
		assertTrue(repositories.get(1) instanceof FixedIndexedRepo);

		FixedIndexedRepo f0 = (FixedIndexedRepo) repositories.get(0);
		assertEquals("_1", f0.getName());
		String resolvedUrl = IO.getFile("testdata/larger-repo.xml").toURI().toString();
		assertEquals(resolvedUrl, f0.getIndexLocations().get(0).toString());

		FixedIndexedRepo f1 = (FixedIndexedRepo) repositories.get(1);
		assertEquals("_2", f1.getName());
		assertEquals("http://example.org/index2.xml", f1.getIndexLocations().get(0).toString());
	}

	public void testExtraAttribs() throws Exception {
		File f = IO.getFile("testdata/standalone/attribs.bndrun");
		Run run = Run.createStandaloneRun(f);

		List<Repository> repositories = run.getWorkspace().getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof FixedIndexedRepo);

		FixedIndexedRepo f0 = (FixedIndexedRepo) repositories.get(0);
		assertEquals("foo", f0.getName());
		assertEquals("http://example.org/index.xml", f0.getIndexLocations().get(0).toString());

		File cacheDir = IO.getFile(System.getProperty("user.home") + "/.custom_cache_dir");
		assertEquals(cacheDir, f0.getCacheDirectory());
	}
}
