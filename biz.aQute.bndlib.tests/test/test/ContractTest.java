package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import org.assertj.core.api.Condition;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ContractTest extends TestCase {

	public void testParameterized() throws Exception {
		Jar bjara = getContractExporter("atest", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);

		a.addClasspath(bjara); // 1x
		a.setProperty(Constants.CONTRACT, "atest;resolution:=optional,*");
		a.setImportPackage("org.osgi.service.cm,*");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		ajar.getManifest()
			.write(System.out);
		assertTrue(a.check());

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertNotNull(p);
		assertEquals(1, p.size());
		Attrs attrs = p.get("osgi.contract");
		String optional = attrs.get("resolution:");
		assertEquals("optional", optional);
		assertEquals("(&(osgi.contract=atest)(version=2.5.0))", attrs.get("filter:"));
	}

	public void testDefinedContract() throws Exception {
		Builder b = newBuilder();
		b.setTrace(true);
		b.setProperty(Constants.FIXUPMESSAGES, "The JAR is empty...");
		b.addClasspath(IO.getFile("jar/jsr311-api-1.1.1.jar"));
		b.setImportPackage("javax.ws.rs.ext");

		b.setProperty(Constants.DEFINE_CONTRACT,
			"osgi.contract;osgi.contract=JavaJAXRS;version:Version=1.1.1;uses:='javax.ws.rs,javax.ws.rs.core,javax.ws.rs.ext'");

		Jar ajar = b.build();
		assertTrue(b.check());
		ajar.getManifest()
			.write(System.out);

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertNotNull(p);
		assertEquals(1, p.size());
		Attrs attrs = p.get("osgi.contract");
		String value = attrs.get("osgi.contract");
		assertEquals("JavaJAXRS", value);
		assertEquals("(&(osgi.contract=JavaJAXRS)(version=1.1.1))", attrs.get("filter:"));
		assertThat(domain.getImportPackage()).containsKey("javax.ws.rs.ext")
			.hasValueSatisfying(new Condition<>(a -> a.get("version") == null, "no version"));
	}

	public void testNoContract() throws Exception {
		Builder b = newBuilder();
		b.setTrace(true);
		b.setProperty(Constants.FIXUPMESSAGES, "The JAR is empty...");
		b.addClasspath(IO.getFile("jar/jsr311-api-1.1.1.jar"));
		b.setImportPackage("javax.ws.rs.ext");

		Jar ajar = b.build();
		assertTrue(b.check());
		ajar.getManifest()
			.write(System.out);

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertTrue(p.isEmpty());
		assertThat(domain.getImportPackage())
			.containsKey("javax.ws.rs.ext")
			.hasValueSatisfying(new Condition<>(a -> a.get("version") != null, "has version"));
	}

	/**
	 * Test the warnings that we have no no version
	 *
	 * @throws Exception
	 */
	public void testWarningVersion() throws Exception {
		Jar bjara = getContractExporter("abc", (String[]) null, "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjara);

		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("test.packageinfo,*");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(
			a.check("Contract \\[name=abc;version=0.0.0;from=biz.aQute.bndlib.tests] does not declare a version"));

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
		a.setImportPackage("test.packageinfo,*");
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
		a.setImportPackage("test.packageinfo,*");
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
		a.setImportPackage("org.osgi.service.cm,*");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check());
		ajar.getManifest()
			.write(System.out);

		Domain domain = Domain.domain(ajar.getManifest());
		Parameters p = domain.getRequireCapability();
		p.remove("osgi.ee");
		assertNotNull(p);
		assertEquals(1, p.size());
		Attrs attrs = p.get("osgi.contract");
		String alpha = attrs.get("alpha");
		assertEquals("1", alpha);
		assertEquals("(&(osgi.contract=atest)(version=2.5.0))", attrs.get("filter:"));
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
		a.setImportPackage("org.osgi.service.cm,*");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check(
			"Contracts \\[Contract \\[name=test;version=2.5.0;from=biz.aQute.bndlib.tests\\], Contract \\[name=test;version=2.5.0"));

	}

	public void testSimple() throws Exception {
		Jar bjar = getContractExporter("test", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjar);
		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("org.osgi.service.cm,*");
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

	public void testMultiple() throws Exception {
		Jar bjar = getContractExporter("abc", new String[] {
			"2.5", "2.6", "3.0", "3.1"
		}, "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjar);
		a.setProperty(Constants.CONTRACT, "*");
		a.setImportPackage("org.osgi.service.cm,*");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check());
		Domain domain = Domain.domain(ajar.getManifest());
		Parameters rc = domain.getRequireCapability();
		rc.remove("osgi.ee");
		System.out.println(rc);
		assertEquals(1, rc.size());
		assertNotNull(rc);
		assertEquals(1, rc.size());
		Attrs attrs = rc.get("osgi.contract");
		assertEquals("(&(osgi.contract=abc)(version=3.1.0))", attrs.get("filter:"));
	}

	public void testSimple_withDefault() throws Exception {
		Jar bjar = getContractExporter("test", "2.5", "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjar);
		a.setImportPackage("org.osgi.service.cm,*");
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
		attrs = rc.get("osgi.contract");
		assertEquals("(&(osgi.contract=test)(version=2.5.0))", attrs.get("filter:"));
	}

	public void testMultiple_withDefault() throws Exception {
		Jar bjar = getContractExporter("abc", new String[] {
			"2.5", "2.6", "3.0", "3.1"
		}, "${exports}");

		Builder a = newBuilder();
		a.setTrace(true);
		a.addClasspath(bjar);
		a.setImportPackage("org.osgi.service.cm,*");
		a.setProperty("Export-Package", "test.refer");
		Jar ajar = a.build();
		assertTrue(a.check());
		Domain domain = Domain.domain(ajar.getManifest());
		Parameters rc = domain.getRequireCapability();
		rc.remove("osgi.ee");
		System.out.println(rc);
		assertEquals(1, rc.size());
		Attrs attrs = rc.get("osgi.contract");
		assertEquals("(&(osgi.contract=abc)(version=3.1.0))", attrs.get("filter:"));
	}

	private Jar getContractExporter(String name, String version, String uses) throws IOException, Exception {
		return getContractExporter(name, new String[] {
			version
		}, uses);
	}

	private Jar getContractExporter(String name, String[] versions, String uses) throws IOException, Exception {
		Builder b = newBuilder();
		Formatter sb = new Formatter();
		try {
			sb.format("osgi.contract");
			if (name != null)
				sb.format(";osgi.contract=%s", name);
			if (versions != null) {
				if (versions.length > 1) {
					StringBuilder s = new StringBuilder(";version:List<Version>=\"");
					for (String version : versions) {
						s.append(version);
						s.append(",");
					}
					s.setLength(s.length() - 1);
					s.append("\"");
					sb.format(s.toString());
				} else
					sb.format(";version:Version=%s", versions[0]);
			}
			if (uses != null)
				sb.format(";uses:='%s'", uses);

			b.setProperty("Provide-Capability", sb.toString());
			b.setProperty("Export-Package", "org.osgi.service.eventadmin,org.osgi.service.cm");
			Jar bjar = b.build();
			assertTrue(b.check());

			return bjar;
		} finally {
			sb.close();
		}
	}

	private Builder newBuilder() throws IOException {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(new File("bin_test"));

		return b;
	}
}
