package org.osgi.service.indexer.impl;

import static org.osgi.service.indexer.impl.Utils.findCaps;
import static org.osgi.service.indexer.impl.Utils.findReqs;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;

import junit.framework.TestCase;

public class TestBundleAnalyzer extends TestCase {

	public void testContentAndIdentity() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/01-bsn+version.jar")), caps, reqs);

		assertEquals(4, caps.size());

		Capability idcap = caps.get(0);
		assertEquals("osgi.identity", idcap.getNamespace());
		assertEquals("org.example.a", idcap.getAttributes().get("osgi.identity"));
		assertEquals("osgi.bundle", idcap.getAttributes().get("type"));
		assertEquals(new Version("0.0.0"), idcap.getAttributes().get("version"));

		Capability content = caps.get(1);
		assertEquals("osgi.content", content.getNamespace());
		assertEquals("64F661EEA43334DC5D38D7F16DBCACD02C799E68332B40E72DA8021828E3329C",
				content.getAttributes().get("osgi.content"));
		assertEquals("testdata/01-bsn+version.jar", content.getAttributes().get("url"));
		assertEquals("application/vnd.osgi.bundle", content.getAttributes().get("mime"));
		assertEquals(1104L, content.getAttributes().get("size"));
	}

	public void testPackageExports() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/03-export.jar")), caps, reqs);

		Capability export = findCaps("osgi.wiring.package", caps).get(0);
		assertEquals("org.example.a", export.getAttributes().get("osgi.wiring.package"));
		assertEquals(new Version(1, 0, 0), export.getAttributes().get("version"));
	}

	public void testPackageExportUses() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/04-export+uses.jar")), caps, reqs);

		List<Capability> exports = findCaps("osgi.wiring.package", caps);
		assertEquals(2, exports.size());

		assertEquals("org.example.b", exports.get(0).getAttributes().get("osgi.wiring.package"));
		assertEquals("org.example.a", exports.get(0).getDirectives().get("uses"));
		assertEquals("org.example.a", exports.get(1).getAttributes().get("osgi.wiring.package"));
	}

	// bundle-symbolic-name and bundle-version must be on package capabilities,
	// for the idiots
	// who add this to their imports...
	public void testPackageExportBundleSymbolicNameAndVersion() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/04-export+uses.jar")), caps, reqs);

		List<Capability> exports = findCaps("osgi.wiring.package", caps);
		assertEquals(2, exports.size());

		assertEquals("org.example.b", exports.get(0).getAttributes().get("osgi.wiring.package"));
		assertEquals("org.example.d", exports.get(0).getAttributes().get("bundle-symbolic-name"));
		assertEquals(new Version(0, 0, 0), exports.get(0).getAttributes().get("bundle-version"));

		assertEquals("org.example.a", exports.get(1).getAttributes().get("osgi.wiring.package"));
		assertEquals("org.example.d", exports.get(0).getAttributes().get("bundle-symbolic-name"));
		assertEquals(new Version(0, 0, 0), exports.get(0).getAttributes().get("bundle-version"));
	}

	public void testPackageImports() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/05-import.jar")), caps, reqs);

		Requirement pkgImport = findReqs("osgi.wiring.package", reqs).get(0);
		assertEquals("(&(osgi.wiring.package=org.example.a)(version>=1.0.0)(!(version>=2.0.0)))",
				pkgImport.getDirectives().get("filter"));
	}

	public void testRequireBundle() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/06-requirebundle.jar")), caps, reqs);

		List<Requirement> requires = findReqs("osgi.wiring.bundle", reqs);
		assertEquals(1, requires.size());
		assertEquals("(&(osgi.wiring.bundle=org.example.a)(bundle-version>=3.0.0)(!(bundle-version>=4.0.0)))",
				requires.get(0).getDirectives().get("filter"));
	}

	public void testPackageImportOptional() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/07-optionalimport.jar")), caps, reqs);

		Requirement pkgImport = findReqs("osgi.wiring.package", reqs).get(0);
		assertEquals("(&(osgi.wiring.package=org.example.a)(version>=1.0.0)(!(version>=2.0.0)))",
				pkgImport.getDirectives().get("filter"));
		assertEquals("optional", pkgImport.getDirectives().get("resolution"));
	}

	public void testFragmentHost() throws Exception {
		BundleAnalyzer a = new BundleAnalyzer(new XNullLogSvc());
		LinkedList<Capability> caps = new LinkedList<>();
		LinkedList<Requirement> reqs = new LinkedList<>();

		a.analyzeResource(new JarResource(new File("testdata/08-fragmenthost.jar")), caps, reqs);

		Requirement req = findReqs("osgi.wiring.host", reqs).get(0);
		assertEquals("(&(osgi.wiring.host=org.example.a)(bundle-version>=0.0.0))", req.getDirectives().get("filter"));
	}

}
