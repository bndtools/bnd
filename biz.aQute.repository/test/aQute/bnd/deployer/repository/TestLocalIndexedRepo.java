package aQute.bnd.deployer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.libg.cryptography.SHA1;
import test.lib.NanoHTTPD;

public class TestLocalIndexedRepo {

	@InjectTemporaryDirectory
	File				outputDir;
	private NanoHTTPD	httpd;
	private int			httpdPort;

	@BeforeEach
	protected void setUp() throws Exception {
		httpd = new NanoHTTPD(0, new File("testdata"));
		httpdPort = httpd.getPort();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		httpd.stop();
	}

	@Test
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

	@Test
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

	@Test
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
