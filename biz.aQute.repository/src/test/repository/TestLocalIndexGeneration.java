package test.repository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import aQute.lib.deployer.repository.AbstractIndexedRepo;
import aQute.lib.deployer.repository.FixedIndexedRepo;
import aQute.lib.deployer.repository.LocalIndexedRepo;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;

public class TestLocalIndexGeneration extends TestCase {

	private Processor reporter;
	private LocalIndexedRepo repo;
	private File outputDir;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.delete(outputDir);
		outputDir.mkdirs();
		
		// Setup the repo
		reporter = new Processor();
		repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "R5");
		repo.setProperties(config);
		repo.setReporter(reporter);
	}

	@Override
	protected void tearDown() throws Exception {
		IO.delete(outputDir);
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
		
		assertEquals(new File("generated/testoutput/name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar").getAbsolutePath(), deployedFile.getAbsolutePath());
		
		File indexFile = new File("generated/testoutput/index.xml.gz");
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
		newRepo.setProperties(config);
		
		return newRepo;
	}
}
