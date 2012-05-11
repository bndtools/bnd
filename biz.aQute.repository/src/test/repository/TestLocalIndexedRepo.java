package test.repository;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import aQute.lib.deployer.repository.LocalIndexedRepo;
import aQute.lib.io.IO;

public class TestLocalIndexedRepo extends TestCase {

	private File outputDir;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.delete(outputDir);
		outputDir.mkdirs();
	}
	
	public void testLocalIndexLocation() throws Exception {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		repo.setProperties(config);
		
		assertEquals(1, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI().toURL(), repo.getIndexLocations().get(0));
	}

	public void testLocalAndRemoteIndexLocations() throws Exception {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("locations", "http://foo/repo.xml,http://bar/repo.xml");
		repo.setProperties(config);
		
		assertEquals(3, repo.getIndexLocations().size());
		assertEquals(new File(outputDir, "index.xml.gz").toURI().toURL(), repo.getIndexLocations().get(0));
		assertEquals(new URL("http://foo/repo.xml"), repo.getIndexLocations().get(1));
		assertEquals(new URL("http://bar/repo.xml"), repo.getIndexLocations().get(2));
	}
}
