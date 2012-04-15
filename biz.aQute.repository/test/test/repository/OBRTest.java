package test.repository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import test.lib.NanoHTTPD;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.deployer.repository.FixedIndexedRepo;
import aQute.libg.version.Version;

public class OBRTest extends TestCase {
	
	private FixedIndexedRepo obr;
	private NanoHTTPD httpd;

	@Override
	protected void setUp() throws Exception {
		obr = new FixedIndexedRepo();
		Map<String, String> config = new HashMap<String, String>();
		config.put("name", "obr");
		config.put("locations", new File("testdata/fullobr.xml").toURI().toString());
		config.put("type", "OBR");
		obr.setProperties(config);
		
		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();
		obr.setCacheDirectory(new File(tmpFile.getAbsolutePath() + ".dir"));
		
		httpd = new NanoHTTPD(18080, new File("testdata/http"));
	}
	
	@Override
	protected void tearDown() throws Exception {
		httpd.stop();
		
		File[] cachedFiles = obr.getCacheDirectory().listFiles();
		if (cachedFiles != null) {
			for (File file : cachedFiles) {
				file.delete();
			}
		}
		obr.getCacheDirectory().delete();
	}
	
	public void testGetLatest() throws Exception {
		File[] files = obr.get("name.njbartlett.osgi.emf.minimal", "latest");
		
		assertNotNull(files);
		assertEquals(1, files.length);
		
		assertEquals("name.njbartlett.osgi.emf.minimal-2.7.0.jar", files[0].getName());
	}
	
	public void testGetAll() throws Exception {
		File[] files = obr.get("name.njbartlett.osgi.emf.xmi", null);
		
		assertNotNull(files);
		assertEquals(2, files.length);
		
		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", files[0].getName());
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", files[1].getName());
	}
	
	public void testGetHttp() throws Exception {
		File[] files = obr.get("org.example.dummy", "latest");
		
		assertNotNull(files);
		assertEquals(1, files.length);
		
		assertNotNull(files[0]);
		assertEquals("dummybundle.jar", files[0].getName());
	}
	
	public void testGetBsnLowest() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", null, Strategy.LOWEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", result.getName());
	}

	public void testGetBsnHighest() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", null, Strategy.HIGHEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", result.getName());
	}
	
	public void testGetBsnLowestWithRange() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "2.5.1", Strategy.LOWEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", result.getName());
	}
	
	public void testGetBsnHighestWithRange() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "[2.5,2.7)", Strategy.HIGHEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", result.getName());
	}
	
	public void testList() throws Exception {
		List<String> result = obr.list("name\\.njbartlett\\..*");
		assertNotNull(result);
		assertEquals(2, result.size());
	}
	
	public void testVersions() throws Exception {
		List<Version> result = obr.versions("name.njbartlett.osgi.emf.minimal");
		assertEquals(2, result.size());
		
		assertEquals(new Version("2.6.1.v20100914-1218"), result.get(0));
		assertEquals(new Version("2.7.0.201104130744"), result.get(1));
	}

}
