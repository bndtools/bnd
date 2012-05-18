package test.repository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.osgi.impl.bundle.bindex.BundleIndexerImpl;

import test.lib.MockRegistry;
import aQute.lib.deployer.repository.AbstractIndexedRepo;
import aQute.lib.deployer.repository.FixedIndexedRepo;
import aQute.lib.deployer.repository.LocalIndexedRepo;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;

public class TestMultipleLocalIndexGeneration extends TestCase {

	private LocalIndexedRepo repo;
	private File outputDir;
	private MockRegistry registry;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.delete(outputDir);
		outputDir.mkdirs();
		
		// Setup the repo
		repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "OBR|R5");
		repo.setProperties(config);

		// Add the BundleIndexer plugin
		MockRegistry registry = new MockRegistry();
		BundleIndexerImpl obrIndexer = new BundleIndexerImpl();
		registry.addPlugin(obrIndexer);
		repo.setRegistry(registry);
	}

	@Override
	protected void tearDown() throws Exception {
		IO.delete(outputDir);
	}

	public void testInitiallyEmpty() throws Exception {
		List<String> list = repo.list(".*");
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	public void testDeployBundle() throws Exception {
		Jar jar = new Jar(new File("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar"));
		File deployedFile = repo.put(jar);
		
		assertEquals(new File("generated/testoutput/name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar").getAbsolutePath(), deployedFile.getAbsolutePath());
		
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
		Map<String, String> config = new HashMap<String, String>();
		config.put("locations", new File("testdata/fullobr.xml").toURI() + "," + new File("testdata/minir5.xml").toURI());
		repo.setProperties(config);
		
		File[] files;
		
		files = repo.get("name.njbartlett.osgi.emf.minimal", "[2.6,2.7)");
		assertEquals(1, files.length);
		assertEquals(new File("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar").getAbsoluteFile(), files[0]);
		
		files = repo.get("dummybundle", null);
		assertEquals(1, files.length);
		assertEquals(new File("testdata/bundles/dummybundle.jar").getAbsoluteFile(), files[0]);
	}
	

	// UTILS

	private static AbstractIndexedRepo createRepoForIndex(File index) {
		FixedIndexedRepo newRepo = new FixedIndexedRepo();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put("locations", index.getAbsoluteFile().toURI().toString());
		newRepo.setProperties(config);
		
		return newRepo;
	}
}
