package test.repository;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.impl.bundle.bindex.*;

import test.lib.*;
import aQute.bnd.deployer.repository.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

public class TestMultipleLocalIndexGeneration extends TestCase {

	private Processor			reporter;
	private LocalIndexedRepo	repo;
	private File				outputDir;
	private MockRegistry		registry;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.deleteWithException(outputDir);
		outputDir.mkdirs();

		// Setup the repo
		reporter = new Processor();
		repo = new LocalIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "OBR|R5");
		repo.setProperties(config);

		// Add the BundleIndexer plugin
		registry = new MockRegistry();
		BundleIndexerImpl obrIndexer = new BundleIndexerImpl();
		registry.addPlugin(obrIndexer);
		repo.setRegistry(registry);

		repo.setReporter(reporter);
	}

	@Override
	protected void tearDown() throws Exception {
		IO.deleteWithException(outputDir);

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testInitiallyEmpty() throws Exception {
		List<String> list = repo.list(".*");
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	public void testDeployBundle() throws Exception {
		Jar jar = new Jar(new File("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar"));
		File deployedFile = repo.put(jar);

		assertEquals(
				new File(
						"generated/testoutput/name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar")
						.getAbsolutePath(), deployedFile.getAbsolutePath());

		File r5IndexFile = new File("generated/testoutput/index.xml.gz");
		assertTrue(r5IndexFile.exists());

		File obrIndexFile = new File("generated/testoutput/repository.xml");
		assertTrue(obrIndexFile.exists());

		AbstractIndexedRepo checkRepo;
		File[] files;

		checkRepo = createRepoForIndex(r5IndexFile);
		files = checkRepo.get("name.njbartlett.osgi.emf.minimal", null);
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(deployedFile.getAbsoluteFile(), files[0]);

		checkRepo = createRepoForIndex(obrIndexFile);
		files = checkRepo.get("name.njbartlett.osgi.emf.minimal", null);
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(deployedFile.getAbsoluteFile(), files[0]);
	}

	public void testReadMixedRepoTypes() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("locations",
				new File("testdata/fullobr.xml").toURI() + "," + new File("testdata/minir5.xml").toURI());
		repo.setProperties(config);

		File[] files;

		files = repo.get("name.njbartlett.osgi.emf.minimal", "[2.6,2.7)");
		assertEquals(1, files.length);
		assertEquals(new File("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar").getAbsoluteFile(),
				files[0]);

		files = repo.get("dummybundle", null);
		assertEquals(1, files.length);
		assertEquals(new File("testdata/bundles/dummybundle.jar").getAbsoluteFile(), files[0]);
	}

	// UTILS

	private static AbstractIndexedRepo createRepoForIndex(File index) {
		FixedIndexedRepo newRepo = new FixedIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("locations", index.getAbsoluteFile().toURI().toString());
		newRepo.setProperties(config);

		return newRepo;
	}
}
