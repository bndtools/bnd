package aQute.bnd.deployer.repository;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

public class TestFixedIndexedRepo extends TestCase {

	private File tmp;

	public void setUp() {
		tmp = IO.getFile("generated/tmp/" + getName());
		IO.delete(tmp);
		tmp.mkdirs();
	}

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
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("name", "index1");
		props.put("locations", IO.getFile("testdata/index1.xml").toURI().toString());
		props.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());

		repo.setProperties(props);
		repo.setReporter(reporter);

		List<String> bsns = repo.list(null);
		Collections.sort(bsns);
		assertEquals(2, bsns.size());
		assertEquals("org.example.c", bsns.get(0));
		assertEquals("org.example.f", bsns.get(1));

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testIndex2() throws Exception {
		Processor reporter = new Processor();

		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("name", "index2");
		props.put("locations", IO.getFile("testdata/index2.xml").toURI().toString());
		props.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(props);
		repo.setReporter(reporter);

		assertEquals(56, countBundles(repo));
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testIndex2Compressed() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("name", "index2");
		props.put("locations", IO.getFile("testdata/index2.xml.gz").toURI().toString());
		props.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(props);
		repo.setReporter(reporter);

		assertEquals(56, countBundles(repo));
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testObr() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("name", "obr");
		config.put("locations", IO.getFile("testdata/fullobr.xml").toURI().toString());
		config.put("type", "OBR");
		config.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);

		File[] files = repo.get("name.njbartlett.osgi.emf.xmi", null);
		assertNotNull(files);
		assertEquals(2, files.length);

		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", files[0].getName());
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", files[1].getName());

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testAmbiguous() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("locations", IO.getFile("testdata/ambiguous.xml").toURI().toString());
		config.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);

		List<String> bsns = repo.list(null);

		assertTrue(reporter.check("Content provider 'OBR'", "Content provider 'R5'", "No content provider matches"));

		assertEquals(0, bsns.size());
	}

	public void testExternalEntitiesNotFetched() throws Exception {
		final AtomicInteger accessCount = new AtomicInteger(0);

		FixedIndexedRepo repo;
		Map<String,String> config;
		Processor reporter = new Processor();

		repo = new FixedIndexedRepo() {
			// A bit of a hack, this makes sure that only OBR can be selected
			protected synchronized void loadAllContentProviders() {
				super.loadAllContentProviders();
				allContentProviders.remove("R5");
			}
		};
		config = new HashMap<String,String>();
		config.put("locations", IO.getFile("testdata/xmlWithDtdRef.xml").getAbsoluteFile().toURI().toString());
		config.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);
		repo.list(null);

		repo = new FixedIndexedRepo() {
			protected synchronized void loadAllContentProviders() {
				super.loadAllContentProviders();
				allContentProviders.remove("OBR");
			}
		};
		config = new HashMap<String,String>();
		config.put("locations", IO.getFile("testdata/xmlWithDtdRef.xml").getAbsoluteFile().toURI().toString());
		config.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);
		repo.list(null);

		assertEquals("Should not make any HTTP connection.", 0, accessCount.get());

		assertTrue("Should be some ambiguity warnings", reporter.getWarnings().size() > 0);
		assertTrue(reporter.check("Content provider 'OBR' was unable to determine",
				"No content provider matches the specified index unambiguously. Selected 'OBR' arbitrarily.",
				"Content provider 'R5' was unable to determine compatibility with index at URL ",
				"No content provider matches the specified index unambiguously. Selected 'R5' arbitrarily"));

		assertEquals("Should not be any errors", 0, reporter.getErrors().size());
	}

	/**
	 * There was a bug in the deployer's fake log service that called the
	 * reporter with a string that could wrongly contain formatting tokens (%),
	 * making it blow up. This checks if we can use spaces in the name.
	 */

	public void testSpaceInName() throws Exception {

		FixedIndexedRepo repo;
		Map<String,String> config;
		Processor reporter = new Processor();

		repo = new FixedIndexedRepo();
		config = new HashMap<String,String>();
		config.put("locations", IO.getFile("testdata/with spaces .xml").getAbsoluteFile().toURI().toString());
		config.put(FixedIndexedRepo.PROP_CACHE, tmp.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);
		repo.list(null);
		assertTrue(reporter.check("Content provider '.*' was unable", "No content provider"));
	}
}
