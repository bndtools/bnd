package aQute.bnd.deployer.obr;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.deployer.repository.providers.ObrUtil;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.helpers.Sed;
import test.lib.NanoHTTPD;

public class OBRTest extends TestCase {

	private static final String	obrSrc	= "testdata/fullobr.xml";
	private static final String	obrDst	= "testdata/fullobr.tmp.xml";
	private static OBR			obr;
	private static NanoHTTPD	httpd;
	private static int			httpdPort;
	private static Processor	reporter;

	@Override
	protected void setUp() throws Exception {
		httpd = new NanoHTTPD(0, IO.getFile("testdata/http"));
		httpdPort = httpd.getPort();

		Sed.file2File(obrSrc, "__httpdPort__", Integer.toString(httpdPort), obrDst);

		obr = new OBR();
		Map<String,String> config = new HashMap<>();
		config.put("location", new File(obrDst).getAbsoluteFile().toURI().toString());

		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();
		File cacheDir = new File(tmpFile.getAbsolutePath() + ".dir");

		obr.setProperties(config);
		obr.setCacheDirectory(cacheDir);

		reporter = new Processor();
		obr.setReporter(reporter);

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
		new File(obrDst).delete();
	}

	public static void testSetProperties() throws Exception {
		OBR obr2 = new OBR();

		Map<String,String> props = new HashMap<>();
		props.put("location", IO.getFile("testdata/fullobr.xml").toURI().toString());
		obr2.setProperties(props);
		obr2.setCacheDirectory(obr.getCacheDirectory());

		Collection<URI> indexes = obr2.getIndexLocations();
		assertEquals(1, indexes.size());
		assertEquals(IO.getFile("testdata/fullobr.xml").toURI().toString(), indexes.iterator().next().toString());

		assertEquals(obr.getCacheDirectory(), obr2.getCacheDirectory());
	}

	public static void testCacheDirectoryNotSpecified() {
		OBR obr2 = new OBR();

		Map<String,String> props = new HashMap<>();
		props.put("location", IO.getFile("testdata/fullobr.xml").toURI().toString());
		obr2.setProperties(props);
	}

	public static void testGetLatest() throws Exception {
		File[] files = obr.get("name.njbartlett.osgi.emf.minimal", "latest");
		assertTrue(reporter.getErrors().isEmpty());

		assertNotNull(files);
		assertEquals(1, files.length);

		assertEquals("name.njbartlett.osgi.emf.minimal-2.7.0.jar", files[0].getName());
	}

	public static void testGetAll() throws Exception {
		File[] files = obr.get("name.njbartlett.osgi.emf.xmi", null);

		assertNotNull(files);
		assertEquals(2, files.length);

		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", files[0].getName());
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", files[1].getName());
	}

	public static void testGetHttp() throws Exception {
		File[] files = obr.get("org.example.dummy", "latest");

		assertNotNull(files);
		assertEquals(1, files.length);

		assertNotNull(files[0]);
		assertEquals("dummybundle.jar", files[0].getName());
	}

	public static void testGetBsnLowest() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", null, Strategy.LOWEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", result.getName());
	}

	public static void testGetBsnHighest() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", null, Strategy.HIGHEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", result.getName());
	}

	public static void testGetBsnLowestWithRange() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "2.5.1", Strategy.LOWEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", result.getName());
	}

	public static void testGetBsnHighestWithRange() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "[2.5,2.7)", Strategy.HIGHEST, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", result.getName());
	}

	/*
	 * public void testGetPackageLowest() throws Exception { Map<String, String>
	 * props = new HashMap<String, String>(); props.put("package",
	 * "org.eclipse.emf.common"); File result = obr.get(null, null,
	 * Strategy.LOWEST, props); assertNotNull(result);
	 * assertEquals("name.njbartlett.osgi.emf.minimal-2.6.1.jar",
	 * result.getName()); } public void testGetPackageLowestWithRange() throws
	 * Exception { Map<String, String> props = new HashMap<String, String>();
	 * props.put("package", "org.eclipse.emf.common"); File result =
	 * obr.get(null, "2.6.2", Strategy.LOWEST, props); assertNotNull(result);
	 * assertEquals("name.njbartlett.osgi.emf.minimal-2.7.0.jar",
	 * result.getName()); } public void testGetPackageHighest() throws Exception
	 * { Map<String, String> props = new HashMap<String, String>();
	 * props.put("package", "org.eclipse.emf.common"); File result =
	 * obr.get(null, null, Strategy.HIGHEST, props); assertNotNull(result);
	 * assertEquals("name.njbartlett.osgi.emf.minimal-2.7.0.jar",
	 * result.getName()); } public void testGetPackageHighestWithRange() throws
	 * Exception { Map<String, String> props = new HashMap<String, String>();
	 * props.put("package", "org.eclipse.emf.common"); File result =
	 * obr.get(null, "[2.6,2.7)", Strategy.HIGHEST, props);
	 * assertNotNull(result);
	 * assertEquals("name.njbartlett.osgi.emf.minimal-2.6.1.jar",
	 * result.getName()); }
	 */

	public static void testList() throws Exception {
		List<String> result = obr.list("name.njbart*");
		assertNotNull(result);
		assertEquals(2, result.size());
	}

	public static void testVersions() throws Exception {
		SortedSet<Version> result = obr.versions("name.njbartlett.osgi.emf.minimal");
		assertEquals(2, result.size());

		assertEquals(new Version("2.6.1.v20100914-1218"), result.first());
		assertEquals(new Version("2.7.0.201104130744"), result.last());
	}

	public static void testName() throws MalformedURLException {
		assertEquals(new File(obrDst).getAbsoluteFile().toURI().toString(), obr.getName());

		OBR obr2 = new OBR();
		Map<String,String> config = new HashMap<>();
		config.put("location", "http://www.example.com/bundles/dummybundle.jar,file:/Users/neil/bundles/dummy.jar");
		obr2.setProperties(config);

		assertEquals("http://www.example.com/bundles/dummybundle.jar,file:/Users/neil/bundles/dummy.jar",
				obr2.getName());
	}

	public static void testProcessFilter1() {
		String filter = "(&(name=foo)(version<1.0.0))";
		String expected = "(&(name=foo)(!(version>=1.0.0)))";
		assertEquals(expected, ObrUtil.processFilter(filter, null));
	}

	public static void testProcessFilter2() {
		String filter = "(&(name=foo)(version>1.0.0))";
		String expected = "(&(name=foo)(!(version<=1.0.0)))";
		assertEquals(expected, ObrUtil.processFilter(filter, null));
	}

	public static void testProcessFilter3() {
		String filter = "(name=foo)(mandatory:<*hello)(version>=1.0.0)(foo<*bar)";
		String expected = "(name=foo)(version>=1.0.0)";
		assertEquals(expected, ObrUtil.processFilter(filter, null));
	}

	public static void testProcessFilter4() {
		String filter = "(name=foo)(mandatory:<*hello)(mandatory:*>goodbye)(version>=1.0.0)(foo<*bar)";
		String expected = "(name=foo)(version>=1.0.0)";
		assertEquals(expected, ObrUtil.processFilter(filter, null));
	}

}
