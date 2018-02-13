package aQute.bnd.deployer.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.deployer.obr.OBR;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class TestLocalObrGeneration extends TestCase {

	private LocalIndexedRepo	repo;
	private File				outputDir;
	private Processor			reporter;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = IO.getFile("generated/tmp/test/" + getName());
		IO.delete(outputDir);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Could not create directory " + outputDir);
		}

		// Setup the repo
		reporter = new Processor();
		repo = new LocalIndexedRepo();
		Map<String,String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "OBR");
		repo.setProperties(config);
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

		File indexFile = IO.getFile(outputDir, "repository.xml");
		assertTrue(indexFile.exists());
		assertTrue(IO.collect(indexFile).length() > 0);

		AbstractIndexedRepo repo2 = createRepoForIndex(indexFile);
		File[] files = repo2.get("name.njbartlett.osgi.emf.minimal", null);
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(deployedFile.getAbsoluteFile(), files[0]);
	}

	// UTILS

	private AbstractIndexedRepo createRepoForIndex(File index) throws Exception {
		OBR repo = new OBR();
		Map<String,String> map = new HashMap<>();
		map.put("locations", index.getAbsoluteFile().toURI().toString());
		map.put("name", getName());
		File cacheDir = new File("generated/tmp/test/cache/" + getName());
		IO.mkdirs(cacheDir);
		map.put("cache", cacheDir.getAbsolutePath());
		map.put("type", "OBR");
		repo.setProperties(map);

		return repo;
	}
}
