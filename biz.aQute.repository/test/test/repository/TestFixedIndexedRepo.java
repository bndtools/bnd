package test.repository;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import junit.framework.*;
import test.lib.*;
import aQute.bnd.deployer.repository.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;

public class TestFixedIndexedRepo extends TestCase {

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

	public static void testIndex1() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("name", "index1");
		props.put("locations", new File("testdata/index1.xml").toURI().toString());
		repo.setProperties(props);
		repo.setReporter(reporter);

		List<String> bsns = repo.list(null);
		assertEquals(2, bsns.size());
		assertEquals("org.example.c", bsns.get(0));
		assertEquals("org.example.f", bsns.get(1));

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public static void testIndex2() throws Exception {
		Processor reporter = new Processor();

		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("name", "index2");
		props.put("locations", new File("testdata/index2.xml").toURI().toString());
		repo.setProperties(props);
		repo.setReporter(reporter);

		assertEquals(56, countBundles(repo));
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public static void testIndex2Compressed() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("name", "index2");
		props.put("locations", new File("testdata/index2.xml.gz").toURI().toString());
		repo.setProperties(props);
		repo.setReporter(reporter);

		assertEquals(56, countBundles(repo));
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public static void testObr() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("name", "obr");
		config.put("locations", new File("testdata/fullobr.xml").toURI().toString());
		config.put("type", "OBR");
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

	public static void testAmbiguous() throws Exception {
		Processor reporter = new Processor();
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("locations", new File("testdata/ambiguous.xml").toURI().toString());
		repo.setProperties(config);
		repo.setReporter(reporter);

		List<String> bsns = repo.list(null);

		assertEquals("Should not be any errors", 0, reporter.getErrors().size());
		assertTrue("Should be some ambiguity warnings", reporter.getWarnings().size() > 0);

		assertEquals(0, bsns.size());
	}

	public static void testExternalEntitiesNotFetched() throws Exception {
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
		config.put("locations", new File("testdata/xmlWithDtdRef.xml").toURI().toString());
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
		config.put("locations", new File("testdata/xmlWithDtdRef.xml").toURI().toString());
		repo.setProperties(config);
		repo.setReporter(reporter);
		repo.list(null);

		assertEquals("Should not make any HTTP connection.", 0, accessCount.get());
		assertEquals("Should not be any errors", 0, reporter.getErrors().size());
		assertTrue("Should be some ambiguity warnings", reporter.getWarnings().size() > 0);
	}

}
