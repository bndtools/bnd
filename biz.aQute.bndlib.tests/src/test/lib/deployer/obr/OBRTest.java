package test.lib.deployer.obr;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.deployer.obr.*;
import aQute.libg.version.*;

public class OBRTest extends TestCase {
	
	private OBR obr;
	private NanoHTTPD httpd;

	@Override
	protected void setUp() throws Exception {
		obr = new OBR();
		obr.setLocations(new URL[] { OBRTest.class.getResource("fullobr.xml") });
		
		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();
		
		obr.setCacheDirectory(new File(tmpFile.getAbsolutePath() + ".dir"));
		
		httpd = new NanoHTTPD(new File("test/http"), 18080);
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
	
	public void testSetProperties() {
		OBR obr2 = new OBR();
		
		Map<String,String> props = new HashMap<String, String>();
		props.put("location", OBRTest.class.getResource("fullobr.xml").toString());
		props.put("cache", this.obr.getCacheDirectory().getAbsolutePath());
		obr2.setProperties(props);
		
		assertEquals(1, obr2.getOBRIndexes().size());
		assertEquals(OBRTest.class.getResource("fullobr.xml").toString(), obr2.getOBRIndexes().get(0).toString());
		
		assertEquals(this.obr.getCacheDirectory(), obr2.getCacheDirectory());
	}
	
	public void testCacheDirectoryNotSpecified() {
		OBR obr2 = new OBR();
		
		Map<String,String> props = new HashMap<String, String>();
		props.put("location", OBRTest.class.getResource("fullobr.xml").toString());
		obr2.setProperties(props);
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
	
	public void testGetPackageLowest() throws Exception {
		Map<String, String> props = new HashMap<String, String>();
		props.put("package", "org.eclipse.emf.common");
		
		File result = obr.get(null, null, Strategy.LOWEST, props);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.minimal-2.6.1.jar", result.getName());
	}

	public void testGetPackageLowestWithRange() throws Exception {
		Map<String, String> props = new HashMap<String, String>();
		props.put("package", "org.eclipse.emf.common");
		
		File result = obr.get(null, "2.6.2", Strategy.LOWEST, props);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.minimal-2.7.0.jar", result.getName());
	}

	public void testGetPackageHighest() throws Exception {
		Map<String, String> props = new HashMap<String, String>();
		props.put("package", "org.eclipse.emf.common");
		
		File result = obr.get(null, null, Strategy.HIGHEST, props);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.minimal-2.7.0.jar", result.getName());
	}

	public void testGetPackageHighestWithRange() throws Exception {
		Map<String, String> props = new HashMap<String, String>();
		props.put("package", "org.eclipse.emf.common");
		
		File result = obr.get(null, "[2.6,2.7)", Strategy.HIGHEST, props);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.minimal-2.6.1.jar", result.getName());
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
	
	public void testName() throws MalformedURLException {
		assertEquals(OBRTest.class.getResource("fullobr.xml").toString(), obr.getName());
		
		OBR obr2 = new OBR();
		obr2.setLocations(new URL[] { new URL("http://www.example.com/bundles/dummybundle.jar"), new URL("file:/Users/neil/bundles/dummy.jar") });
		
		assertEquals("http://www.example.com/bundles/dummybundle.jar,file:/Users/neil/bundles/dummy.jar", obr2.getName());
	}
}
