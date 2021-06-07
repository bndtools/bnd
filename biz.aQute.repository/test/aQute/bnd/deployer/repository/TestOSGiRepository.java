package aQute.bnd.deployer.repository;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class TestOSGiRepository extends TestCase {

	private File tmp;

	@Override
	public void setUp() {
		tmp = IO.getFile("generated/tmp/test/" + getClass().getName() + "/" + getName());
		IO.delete(tmp);
		tmp.mkdirs();
	}

	@Override
	public void tearDown() {
		IO.delete(tmp);
	}

	private static int countBundles(RepositoryPlugin repo) throws Exception {
		int count = 0;

		List<String> list = repo.list(null);
		if (list != null)
			for (String bsn : list) {
				SortedSet<Version> versions = repo.versions(bsn);
				if (versions != null)
					count += versions.size();
			}

		return count;
	}

	public void testIndex1() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> props = new HashMap<>();
			props.put("name", getName());
			props.put("locations", IO.getFile("testdata/index1.xml")
				.toURI()
				.toString());
			props.put("cache", tmp.getAbsolutePath());

			repo.setProperties(props);
			repo.setReporter(reporter);

			List<String> bsns = repo.list(null);
			Collections.sort(bsns);
			assertEquals(2, bsns.size());
			assertEquals("org.example.c", bsns.get(0));
			assertEquals("org.example.f", bsns.get(1));

			assertEquals(0, reporter.getErrors()
				.size());
			assertEquals(0, reporter.getWarnings()
				.size());
		}
	}

	public void testIndex2() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> props = new HashMap<>();
			props.put("name", getName());
			props.put("locations", IO.getFile("testdata/index2.xml")
				.toURI()
				.toString());
			props.put("cache", tmp.getAbsolutePath());
			repo.setProperties(props);
			repo.setReporter(reporter);

			assertEquals(56, countBundles(repo));
			assertEquals(0, reporter.getErrors()
				.size());
			assertEquals(0, reporter.getWarnings()
				.size());
		}
	}

	public void testIndex2Compressed() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> props = new HashMap<>();
			props.put("name", getName());
			props.put("locations", IO.getFile("testdata/index2.xml.gz")
				.toURI()
				.toString());
			props.put("cache", tmp.getAbsolutePath());
			repo.setProperties(props);
			repo.setReporter(reporter);

			assertEquals(56, countBundles(repo));
			assertEquals(0, reporter.getErrors()
				.size());
			assertEquals(0, reporter.getWarnings()
				.size());
		}
	}

	public void testAmbiguous() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> config = new HashMap<>();
			config.put("locations", IO.getFile("testdata/ambiguous.xml")
				.toURI()
				.toString());
			config.put("name", getName());
			config.put("cache", tmp.getAbsolutePath());
			repo.setProperties(config);
			repo.setReporter(reporter);

			List<String> bsns = repo.list(null);

			assertTrue(reporter.check());

			assertEquals(0, bsns.size());
		}
	}

	/**
	 * There was a bug in the deployer's fake log service that called the
	 * reporter with a string that could wrongly contain formatting tokens (%),
	 * making it blow up. This checks if we can use spaces in the name.
	 */

	public void testSpaceInName() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> config = new HashMap<>();
			config.put("locations", IO.getFile("testdata/with spaces .xml")
				.getAbsoluteFile()
				.toURI()
				.toString());
			config.put("name", getName());
			config.put("cache", tmp.getAbsolutePath());
			repo.setProperties(config);
			repo.setReporter(reporter);
			repo.list(null);
			assertTrue(reporter.check());
		}
	}
}
