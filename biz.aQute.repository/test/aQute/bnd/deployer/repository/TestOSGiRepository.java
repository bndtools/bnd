package aQute.bnd.deployer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

public class TestOSGiRepository {

	private String	name;
	private File tmp;

	@BeforeEach
	public void setUp(TestInfo testInfo) {
		name = testInfo.getTestMethod()
			.get()
			.getName();
		tmp = IO.getFile("generated/tmp/test/" + testInfo.getTestClass()
			.get()
			.getName() + "/"
			+ name)
			.getAbsoluteFile();
		IO.delete(tmp);
		tmp.mkdirs();
	}

	@AfterEach
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

	@Test
	public void testIndex1() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> props = new HashMap<>();
			props.put("name", name);
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

	@Test
	public void testIndex2() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> props = new HashMap<>();
			props.put("name", name);
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

	@Test
	public void testIndex2Compressed() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> props = new HashMap<>();
			props.put("name", name);
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

	@Test
	public void testAmbiguous() throws Exception {
		Processor reporter = new Processor();
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			reporter.addBasicPlugin(httpClient);
			repo.setRegistry(reporter);
			Map<String, String> config = new HashMap<>();
			config.put("locations", IO.getFile("testdata/ambiguous.xml")
				.toURI()
				.toString());
			config.put("name", name);
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

	@Test
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
			config.put("name", name);
			config.put("cache", tmp.getAbsolutePath());
			repo.setProperties(config);
			repo.setReporter(reporter);
			repo.list(null);
			assertTrue(reporter.check());
		}
	}
}
