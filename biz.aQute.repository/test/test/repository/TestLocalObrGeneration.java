package test.repository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.impl.bundle.bindex.BundleIndexerImpl;

import test.lib.MockRegistry;

import junit.framework.TestCase;
import aQute.lib.deployer.repository.AbstractIndexedRepo;
import aQute.lib.deployer.repository.FixedIndexedRepo;
import aQute.lib.deployer.repository.LocalIndexedRepo;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;

public class TestLocalObrGeneration extends TestCase {

	private LocalIndexedRepo repo;
	private File outputDir;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.delete(outputDir);
		outputDir.mkdirs();
		
		// Setup the repo
		repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "OBR");
		repo.setProperties(config);
		
		// Add the BundleIndexer plugin
		MockRegistry registry = new MockRegistry();
		BundleIndexerImpl obrIndexer = new BundleIndexerImpl();
		registry.addPlugin(obrIndexer);
		repo.setRegistry(registry);
	}

	@Override
	protected void tearDown() throws Exception {
//		IO.delete(outputDir);
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
		
		File indexFile = new File("generated/testoutput/repository.xml");
		assertTrue(indexFile.exists());
		
		AbstractIndexedRepo repo2 = createRepoForIndex(indexFile);
		File[] files = repo2.get("name.njbartlett.osgi.emf.minimal", null);
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(deployedFile.getAbsoluteFile(), files[0]);
	}

	// UTILS

	private static AbstractIndexedRepo createRepoForIndex(File index) {
		FixedIndexedRepo newRepo = new FixedIndexedRepo();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put("locations", index.getAbsoluteFile().toURI().toString());
		config.put("type", "OBR");
		newRepo.setProperties(config);
		
		return newRepo;
	}
}
