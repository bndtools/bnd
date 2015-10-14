package aQute.bnd.deployer.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.lib.NanoHTTPD;

public class TestLocalIndexedRepo extends TestCase {

	private File		outputDir;
	private NanoHTTPD	httpd;
	private int			httpdPort;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = IO.getFile("generated/testoutput/" + getName());
		IO.deleteWithException(outputDir);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Could not create directory " + outputDir);
		}

		httpd = new NanoHTTPD(0, new File("testdata"));
		httpdPort = httpd.getPort();
	}

	@Override
	protected void tearDown() throws Exception {
		httpd.stop();
	}

	public void testInlcudePolicy_Default() {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		HashMap<String,String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		repo.setProperties(config);

		Pattern policy = repo.includePolicy();
		assertEquals(policy.toString(), LocalIndexedRepo.VERSIONED_BUNDLE_PATTERN);
	}

	public void testInlcudePolicy_AllJars() {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		HashMap<String,String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, "ALL_JARS");
		repo.setProperties(config);

		Pattern policy = repo.includePolicy();
		assertEquals(policy.toString(), LocalIndexedRepo.ALL_JARS_PATTERN);
	}

	public void testInlcudePolicy_AllJarsAndLibs() {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		HashMap<String,String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, "ALL_JARS_AND_LIBS");
		repo.setProperties(config);

		Pattern policy = repo.includePolicy();
		assertEquals(policy.toString(), LocalIndexedRepo.ALL_JARS_AND_LIBS_PATTERN);
	}

	public void testInlcudePolicy_VersionedBundle() {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		HashMap<String,String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, "VERSIONED_BUNDLE");
		repo.setProperties(config);

		Pattern policy = repo.includePolicy();
		assertEquals(policy.toString(), LocalIndexedRepo.VERSIONED_BUNDLE_PATTERN);
	}

	public void testInlcudePolicy_Regex() {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		HashMap<String,String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, ".*(foo|bar)$");
		repo.setProperties(config);

		Pattern policy = repo.includePolicy();
		assertEquals(policy.toString(), ".*(foo|bar)$");
	}

	public void testIncludePolicy_AllJars_ListFiles() throws Exception {
		File file = IO.getFile("testdata/LocalIndexedRepo");
		LocalIndexedRepo repo = new LocalIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("local", file.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, "ALL_JARS");

		repo.setProperties(config);

		Set<File> files = new HashSet<>();
		repo.gatherFiles(files);

		File aJar = IO.getFile("testdata/LocalIndexedRepo/a.jar");
		File bJar = IO.getFile("testdata/LocalIndexedRepo/b.jar");
		assertTrue(files.contains(aJar));
		assertTrue(files.contains(bJar));
		assertEquals(files.size(), 2);
	}

	public void testIncludePolicy_AllJarsAndLibs_ListFiles() throws Exception {
		File file = IO.getFile("testdata/LocalIndexedRepo");
		LocalIndexedRepo repo = new LocalIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("local", file.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, "ALL_JARS_AND_LIBS");

		repo.setProperties(config);

		Set<File> files = new HashSet<>();
		repo.gatherFiles(files);

		File aJar = IO.getFile("testdata/LocalIndexedRepo/a.jar");
		File bJar = IO.getFile("testdata/LocalIndexedRepo/b.jar");
		File aLib = IO.getFile("testdata/LocalIndexedRepo/a.lib");
		File bLib = IO.getFile("testdata/LocalIndexedRepo/b.lib");
		assertTrue(files.contains(aJar));
		assertTrue(files.contains(bJar));
		assertTrue(files.contains(aLib));
		assertTrue(files.contains(bLib));
		assertEquals(files.size(), 4);
	}

	public void testIncludePolicy_Regex_ListFiles() throws Exception {
		File file = IO.getFile("testdata/LocalIndexedRepo");
		LocalIndexedRepo repo = new LocalIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("local", file.getAbsolutePath());
		config.put(LocalIndexedRepo.PROP_FILE_INCLUDE_POLICY, ".*");

		repo.setProperties(config);

		Set<File> files = new HashSet<>();
		repo.gatherFiles(files);

		File aJar = IO.getFile("testdata/LocalIndexedRepo/a.jar");
		File bJar = IO.getFile("testdata/LocalIndexedRepo/b.jar");
		File aLib = IO.getFile("testdata/LocalIndexedRepo/a.lib");
		File bLib = IO.getFile("testdata/LocalIndexedRepo/b.lib");
		File aProp = IO.getFile("testdata/LocalIndexedRepo/a.props");
		File bProp = IO.getFile("testdata/LocalIndexedRepo/b.props");
		assertTrue(files.contains(aJar));
		assertTrue(files.contains(bJar));
		assertTrue(files.contains(aLib));
		assertTrue(files.contains(bLib));
		assertTrue(files.contains(aProp));
		assertTrue(files.contains(bProp));
		assertEquals(files.size(), 6);
	}

	public void testLocalIndexLocation() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);

		assertEquals(1, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI(), repo.getIndexLocations().get(0));
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testLocalAndRemoteIndexLocations() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("locations", "http://localhost:" + httpdPort + "/index1.xml,http://localhost:" + httpdPort + "/index2.xml");
		repo.setProperties(config);
		repo.setReporter(reporter);

		assertEquals(3, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI(), repo.getIndexLocations().get(0));
		assertEquals(new URI("http://localhost:" + httpdPort + "/index1.xml"), repo.getIndexLocations().get(1));
		assertEquals(new URI("http://localhost:" + httpdPort + "/index2.xml"), repo.getIndexLocations().get(2));

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}
}
