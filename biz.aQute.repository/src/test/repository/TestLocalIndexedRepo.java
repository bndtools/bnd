package test.repository;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import test.lib.NanoHTTPD;

import junit.framework.TestCase;
import aQute.lib.deployer.repository.LocalIndexedRepo;
import aQute.lib.io.IO;
import aQute.lib.osgi.Processor;

public class TestLocalIndexedRepo extends TestCase {

	private File outputDir;
	private NanoHTTPD httpd;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.delete(outputDir);
		outputDir.mkdirs();
		
		httpd = new NanoHTTPD(18081, new File("testdata"));
	}
	
	@Override
	protected void tearDown() throws Exception {
		httpd.stop();
	}
	
	public void testLocalIndexLocation() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		repo.setProperties(config);
		repo.setReporter(reporter);
		
		assertEquals(1, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI().toURL(), repo.getIndexLocations().get(0));
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testLocalAndRemoteIndexLocations() throws Exception {
		Processor reporter = new Processor();
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("locations", "http://localhost:18081/index1.xml,http://localhost:18081/index2.xml");
		repo.setProperties(config);
		repo.setReporter(reporter);
		
		assertEquals(3, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI().toURL(), repo.getIndexLocations().get(0));
		assertEquals(new URL("http://localhost:18081/index1.xml"), repo.getIndexLocations().get(1));
		assertEquals(new URL("http://localhost:18081/index2.xml"), repo.getIndexLocations().get(2));
		
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}
}
