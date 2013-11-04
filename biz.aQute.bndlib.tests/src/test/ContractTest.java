package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;

public class ContractTest extends TestCase {

	/**
	 * Test the warnings that we have no no version
	 * 
	 * @throws Exception
	 */
	public void testWarningVersion() throws Exception {
		Jar bjara = getContractExporter("abc", null, "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjara);

		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("test.packageinfo");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check("Contract \\[name=abc;version=0.0.0;from=biz.aQute.bndlib.tests] does not declare a version"));

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertEquals(0, p.size());
	}
	/**
	 * Test the warnings that we have no uses
	 * 
	 * @throws Exception
	 */
	public void testWarningUses() throws Exception {
		Jar bjara = getContractExporter("abc", "2.5", null);

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjara);

		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("test.packageinfo");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check("Contract abc has no uses: directive"));

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertEquals(0, p.size());
	}

	/**
	 * Make sure we do not add a contract if not used
	 * 
	 * @throws Exception
	 */
	public void testUnused() throws Exception {
		Jar bjara = getContractExporter("atest", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjara);

		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("test.packageinfo");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check());

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertEquals(0, p.size());
	}

	/**
	 * Test if we can select
	 * 
	 * @throws Exception
	 */
	public void testSelect() throws Exception {
		Jar bjara = getContractExporter("atest", "2.5", "${exports}");
		Jar bjarb = getContractExporter("btest", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);

		a.addClasspath(bjara); // 1x
		a.addClasspath(bjarb); // 2x
		a.setProperty(Constants.CONTRACT, "atest;alpha=1");
		a.setImportPackage("org.osgi.service.cm");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check());
		ajar.getManifest().write(System.out);

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertNotNull(p);
		assertEquals(1, p.size());
		Attrs attrs = p.get("osgi.contract");
		String alpha = attrs.get("alpha");
		assertEquals("1", alpha);
		assertEquals("(&(osgi.contract=atest)(&(version>=2.5.0)(!(version>=3.0.0))))", attrs.get("filter:"));
	}

	/**
	 * Test if we can detect an overlap, and then if we can control the overlap
	 * 
	 * @throws Exception
	 */
	public void testOverlap() throws Exception {
		Jar bjar = getContractExporter("test", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);

		a.addClasspath(bjar); // 1x
		a.addClasspath(bjar); // 2x
		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("org.osgi.service.cm");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a
				.check("Contracts \\[Contract \\[name=test;version=2.5.0;from=biz.aQute.bndlib.tests\\], Contract \\[name=test;version=2.5.0"));

	}

	public void testSimple() throws Exception {
		Jar bjar = getContractExporter("test", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjar);
		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("org.osgi.service.cm");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check());
		Domain domain = Domain.domain(ajar.getManifest());
		Parameters rc = domain.getRequireCapability();
		rc.remove("osgi.ee");
		System.out.println(rc);
		assertEquals(1, rc.size());

		Packages ps = a.getImports();
		assertTrue(ps.containsFQN("org.osgi.service.cm"));
		Attrs attrs = ps.getByFQN("org.osgi.service.cm");
		assertNotNull(attrs);
		assertNull(attrs.getVersion());
	}

	private Jar getContractExporter(String name, String version, String uses) throws IOException, Exception {
		Builder b = newBuilder();
		Formatter sb = new Formatter();
		try {
			sb.format("osgi.contract");
			if (name != null)
				sb.format(";osgi.contract=%s", name);
			if (version != null)
				sb.format(";version:Version=%s", version);
			if (uses != null)
				sb.format(";uses:='%s'", uses);

			b.setProperty("Provide-Capability", sb.toString());
			b.setProperty("Export-Package", "org.osgi.service.eventadmin,org.osgi.service.cm");
			Jar bjar = b.build();
			assertTrue(b.check());

			return bjar;
		}
		finally {
			sb.close();
		}
	}

	private Builder newBuilder() throws IOException {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.addClasspath(new File("bin"));

		return b;
	}
}
