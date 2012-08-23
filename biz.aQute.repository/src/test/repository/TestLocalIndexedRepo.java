package test.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import test.lib.*;
import aQute.bnd.deployer.repository.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

public class TestLocalIndexedRepo extends TestCase {

	private static File		outputDir;
	private static NanoHTTPD	httpd;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated" + File.separator + "testoutput");
		IO.deleteWithException(outputDir);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Could not create directory " + outputDir);
		}

		httpd = new NanoHTTPD(18081, new File("testdata"));
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
		config.put("locations", "http://localhost:18081/index1.xml,http://localhost:18081/index2.xml");
		repo.setProperties(config);
		repo.setReporter(reporter);

		assertEquals(3, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI(), repo.getIndexLocations().get(0));
		assertEquals(new URI("http://localhost:18081/index1.xml"), repo.getIndexLocations().get(1));
		assertEquals(new URI("http://localhost:18081/index2.xml"), repo.getIndexLocations().get(2));

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}
}
