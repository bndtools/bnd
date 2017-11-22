package aQute.bnd.deployer.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.impl.bundle.bindex.BundleIndexerImpl;

import aQute.bnd.deployer.obr.OBR;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.lib.MockRegistry;

public class TestMultipleLocalIndexGeneration extends TestCase {

	private Processor			reporter;
	private LocalIndexedRepo	repo;
	private File				outputDir;
	private MockRegistry		registry;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = IO.getFile("generated/tmp/test/" + getName());
		IO.deleteWithException(outputDir);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Could not create directory " + outputDir);
		}

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
		PutResult r = repo.put(
				new BufferedInputStream(
						new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
				new RepositoryPlugin.PutOptions());
		File deployedFile = new File(r.artifact);

		assertEquals(
				IO.getFile(outputDir, "name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar")
						.getAbsolutePath(),
				deployedFile.getAbsolutePath());

		File r5IndexFile = IO.getFile(outputDir, "index.xml.gz");
		assertTrue(r5IndexFile.exists());

		File obrIndexFile = IO.getFile(outputDir, "repository.xml");
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
		OBR repo = new OBR();
		Map<String,String> config = new HashMap<String,String>();
		config.put("locations",
				IO.getFile("testdata/fullobr.xml").toURI() + "," + IO.getFile("testdata/minir5.xml").toURI());
		repo.setProperties(config);

		File[] files;

		files = repo.get("name.njbartlett.osgi.emf.minimal", "[2.6,2.7)");
		assertEquals(1, files.length);
		assertEquals(IO.getFile("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar").getAbsoluteFile(),
				files[0]);

		files = repo.get("dummybundle", null);
		assertEquals(1, files.length);
		assertEquals(IO.getFile("testdata/bundles/dummybundle.jar").getAbsoluteFile(), files[0]);
	}

	// UTILS

	private AbstractIndexedRepo createRepoForIndex(File index) throws Exception {
		OBR newRepo = new OBR();

		Map<String,String> config = new HashMap<String,String>();
		config.put("locations", index.getAbsoluteFile().toURI().toString());
		config.put("name", getName());
		File cacheDir = new File("generated/tmp/test/cache/" + getName());
		IO.mkdirs(cacheDir);
		config.put("cache", cacheDir.getAbsolutePath());
		newRepo.setProperties(config);

		return newRepo;
	}
}
