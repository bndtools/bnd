package aQute.bnd.deployer.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import test.lib.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

public class TestLocalIndexedRepo extends TestCase {

	private static File		outputDir;
	private static NanoHTTPD	httpd;
	private static int			httpdPort;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated" + File.separator + "testoutput");
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

	public static void testLocalIndexLocation() throws Exception {
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

	public static void testLocalAndRemoteIndexLocations() throws Exception {
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
