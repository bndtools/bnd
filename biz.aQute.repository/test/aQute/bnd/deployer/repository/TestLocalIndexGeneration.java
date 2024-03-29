package aQute.bnd.deployer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA256;
import test.lib.MockRegistry;
import test.repository.FailingGeneratingProvider;
import test.repository.NonGeneratingProvider;

@SuppressWarnings("resource")
public class TestLocalIndexGeneration {

	private Processor				reporter;
	private LocalIndexedRepo		repo;
	@InjectTemporaryDirectory
	File							outputDir;
	private HashMap<String, String>	config;

	@BeforeEach
	protected void setUp() throws Exception {
		// Setup the repo
		reporter = new Processor();
		repo = new LocalIndexedRepo();
		config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "R5");
		config.put("pretty", "true");
		repo.setProperties(config);
		repo.setReporter(reporter);
	}

	@AfterEach
	protected void tearDown() throws Exception {
		assertEquals(0, reporter.getErrors()
			.size());
		assertEquals(0, reporter.getWarnings()
			.size());
	}

	@Test
	public void testInitiallyEmpty() throws Exception {
		List<String> list = repo.list(".*");
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	public void testDeployBundle() throws Exception {
		PutResult r = repo.put(
			new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
			new RepositoryPlugin.PutOptions());
		File deployedFile = new File(r.artifact);

		assertEquals(
			IO.getFile(outputDir, "name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar")
				.getAbsolutePath(),
			deployedFile.getAbsolutePath());

		File indexFile = IO.getFile(outputDir, "index.xml");
		assertTrue(indexFile.exists());

		File indexFileSha = IO.getFile(outputDir, "index.xml.sha");
		assertTrue(indexFileSha.exists());

		try (OSGiRepository repo2 = createRepoForIndex(indexFile)) {
			SortedSet<Version> versions = repo2.versions("name.njbartlett.osgi.emf.minimal");
			assertNotNull(versions);
			assertEquals(1, versions.size());
			File file = repo2.get("name.njbartlett.osgi.emf.minimal", versions.first(), null);
			assertNotNull(file);
			assertEquals(SHA256.digest(deployedFile), SHA256.digest(file));
		}
	}

	@Test
	public void testOverwrite() throws Exception {
		config.put("overwrite", "false");
		repo.setProperties(config);

		PutResult r = repo.put(
			new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
			new RepositoryPlugin.PutOptions());
		File originalFile = new File(r.artifact);
		assertEquals(
			IO.getFile(outputDir, "name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar")
				.getAbsolutePath(),
			originalFile.getAbsolutePath());

		Jar newJar = new Jar(IO.getFile("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar"));
		Jar dummyJar = new Jar(IO.getFile("testdata/bundles/dummybundle.jar"));
		newJar.putResource("testOverwrite/dummybundle.jar", new JarResource(dummyJar));
		newJar.write("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1-testOverwrite.jar");
		r = repo.put(
			new BufferedInputStream(
				new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1-testOverwrite.jar")),
			new RepositoryPlugin.PutOptions());
		IO.delete(IO.getFile("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1-testOverwrite.jar"));
		assertNull(r.artifact);
	}

	@Test
	public void testInvalidContentProvider() throws Exception {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "Rubbish");
		repo.setProperties(config);
		repo.setReporter(reporter);

		repo.put(
			new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
			new RepositoryPlugin.PutOptions());

		assertEquals(0, reporter.getErrors()
			.size());
		assertTrue(reporter.getWarnings()
			.size() > 0);
		reporter.clear();
	}

	@Test
	public void testNonGeneratingProvider() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(new NonGeneratingProvider());

		LocalIndexedRepo repo = new LocalIndexedRepo();
		repo.setRegistry(registry);
		repo.setReporter(reporter);

		Map<String, String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "Nongenerating");
		repo.setProperties(config);

		repo.put(
			new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
			new RepositoryPlugin.PutOptions());

		assertEquals(0, reporter.getErrors()
			.size());
		assertTrue(reporter.getWarnings()
			.size() > 0);
		reporter.clear();
	}

	@Test
	public void testFailToGenerate() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(new FailingGeneratingProvider());

		LocalIndexedRepo repo = new LocalIndexedRepo();
		repo.setRegistry(registry);
		repo.setReporter(reporter);

		Map<String, String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "Fail");
		repo.setProperties(config);

		repo.put(
			new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
			new RepositoryPlugin.PutOptions());

		assertTrue(reporter.getErrors()
			.size() > 0);
		assertEquals(0, reporter.getWarnings()
			.size());
		reporter.clear();
	}

	@Test
	public void testUncompressedIndexFile() throws Exception {
		repo = new LocalIndexedRepo();
		config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "R5");
		config.put("pretty", "true");
		config.put("compressed", "false");
		repo.setProperties(config);
		repo.setReporter(reporter);

		PutResult r = repo.put(
			new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")),
			new RepositoryPlugin.PutOptions());
		File deployedFile = new File(r.artifact);

		File compressedIndexFile = IO.getFile(outputDir, "index.xml.gz");
		assertFalse(compressedIndexFile.exists());

		File prettyIndexFile = IO.getFile(outputDir, "index.xml");
		assertTrue(prettyIndexFile.exists());

		try {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(prettyIndexFile));
			fail("expected opening gzip on index file would fail because it should be uncompressed");
		} catch (ZipException ze) {} finally {
			IO.delete(new File(r.artifact));
			IO.delete(prettyIndexFile);
		}
	}

	// UTILS

	private OSGiRepository createRepoForIndex(File index) throws Exception {
		OSGiRepository repo = new OSGiRepository();
		HttpClient httpClient = new HttpClient();
		Map<String, String> map = new HashMap<>();
		map.put("locations", index.getAbsoluteFile()
			.toURI()
			.toString());
		String name = outputDir.getName();
		map.put("name", name);
		map.put("cache", new File(outputDir.getParentFile(), "cache/" + name).getAbsolutePath());
		repo.setProperties(map);
		Processor p = new Processor();
		p.addBasicPlugin(httpClient);
		repo.setRegistry(p);

		return repo;
	}
}
