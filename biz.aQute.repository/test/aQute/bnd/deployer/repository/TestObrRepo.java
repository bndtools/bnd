package aQute.bnd.deployer.repository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.deployer.obr.OBR;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.helpers.Sed;
import test.lib.NanoHTTPD;

public class TestObrRepo extends TestCase {

	private static final String		obrSrc	= "testdata/fullobr.xml";
	private static final String		obrDst	= "testdata/fullobr.tmp.xml";
	private static OBR			obr;
	private static NanoHTTPD		httpd;
	private static int				httpdPort;
	private static Processor		reporter;

	@Override
	protected void setUp() throws Exception {
		httpd = new NanoHTTPD(0, IO.getFile("testdata/http"));
		httpdPort = httpd.getPort();

		Sed.file2File(obrSrc, "__httpdPort__", Integer.toString(httpdPort), obrDst);

		reporter = new Processor();
		obr = new OBR();
		Map<String,String> config = new HashMap<String,String>();
		config.put("name", "obr");
		config.put("locations", new File(obrDst).toURI().toString());
		config.put("type", "OBR");
		obr.setProperties(config);
		obr.setReporter(reporter);

		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();
		obr.setCacheDirectory(new File(tmpFile.getAbsolutePath() + ".dir"));
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

		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public static void testGetLatest() throws Exception {
		File[] files = obr.get("name.njbartlett.osgi.emf.minimal", "latest");

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

	public static void testGetBsnExcactNoMatch() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "2.4.0", Strategy.EXACT, null);
		assertNull(result);
	}

	public static void testGetBsnExcactWithQualifier() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "2.7.0.201104130744", Strategy.EXACT, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", result.getName());
	}

	public static void testGetBsnExcact() throws Exception {
		File result = obr.get("name.njbartlett.osgi.emf.xmi", "2.7.0", Strategy.EXACT, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.7.0.jar", result.getName());

		result = obr.get("name.njbartlett.osgi.emf.xmi", "2.5.0", Strategy.EXACT, null);
		assertNotNull(result);
		assertEquals("name.njbartlett.osgi.emf.xmi-2.5.0.jar", result.getName());
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

}
