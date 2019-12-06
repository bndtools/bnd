package aQute.bnd.deployer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import junit.framework.TestCase;
import test.lib.NanoHTTPD;

public class TestLocalIndexedRepo extends TestCase {

	private File		outputDir;
	private NanoHTTPD	httpd;
	private int			httpdPort;

	@Override
	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = IO.getFile("generated/tmp/test/" + getClass().getName() + "/" + getName());
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

	public void testLocalIndexLocation() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);

		assertEquals(1, repo.getIndexLocations()
			.size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI(), repo.getIndexLocations()
			.get(0));
		assertTrue(reporter.check());
	}

	public void testLocalIndexLocationWithPretty() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("pretty", true + "");
		repo.setProperties(config);
		repo.setReporter(reporter);

		assertEquals(1, repo.getIndexLocations()
			.size());
		File indexFile = new File(outputDir, "index.xml");
		assertEquals(indexFile.toURI(), repo.getIndexLocations()
			.get(0));
		assertTrue(reporter.check());

		SHA1 digest = SHA1.digest(indexFile);

		Thread.sleep(1000);
		repo.refresh();

		assertEquals(1, repo.getIndexLocations()
			.size());

		// Check file not touched
		assertThat(indexFile).hasDigest("SHA1", digest.digest());
	}

	public void testLocalAndRemoteIndexLocations() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("locations",
			"http://localhost:" + httpdPort + "/index1.xml,http://localhost:" + httpdPort + "/index2.xml");
		repo.setProperties(config);
		repo.setReporter(reporter);

		assertEquals(3, repo.getIndexLocations()
			.size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI(), repo.getIndexLocations()
			.get(0));
		assertEquals(new URI("http://localhost:" + httpdPort + "/index1.xml"), repo.getIndexLocations()
			.get(1));
		assertEquals(new URI("http://localhost:" + httpdPort + "/index2.xml"), repo.getIndexLocations()
			.get(2));

		assertEquals(0, reporter.getErrors()
			.size());
		assertEquals(0, reporter.getWarnings()
			.size());
	}
}
