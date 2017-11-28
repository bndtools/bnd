package aQute.bnd.deployer.repository.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.impl.bundle.bindex.BundleIndexerImpl;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.deployer.repository.NullLogService;
import aQute.bnd.deployer.repository.api.IRepositoryIndexProcessor;
import aQute.bnd.deployer.repository.api.Referral;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class TestObrCapReqParsing extends TestCase {

	private static List<Resource> parseIndex(InputStream stream, URI baseUri) throws Exception {
		ObrContentProvider parser = new ObrContentProvider(new BundleIndexerImpl());
		final List<Resource> resources = new LinkedList<Resource>();
		IRepositoryIndexProcessor processor = new IRepositoryIndexProcessor() {
			public void processResource(Resource resource) {
				resources.add(resource);
			}

			public void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth) {}
		};
		parser.parseIndex(stream, baseUri, processor, new NullLogService());
		return resources;
	}

	public static void testObrContentCaps() throws Exception {
		FileInputStream stream = new FileInputStream("testdata/fullobr.xml");
		URI baseUri = new File("testdata").toURI();
		List<Resource> resources = parseIndex(stream, baseUri);

		assertEquals(7, resources.size());

		Resource resource = resources.get(0);

		// Check identity
		List<Capability> idCaps = resource.getCapabilities("osgi.identity");
		assertEquals(1, idCaps.size());
		assertEquals("name.njbartlett.osgi.emf.minimal", idCaps.get(0).getAttributes().get("osgi.identity"));

		// Check content
		List<Capability> contentCaps = resource.getCapabilities("osgi.content");
		assertEquals(1, contentCaps.size());
		assertEquals(
				IO.getFile("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.7.0.jar")
						.getAbsoluteFile()
						.toURI()
						.toString(),
				contentCaps.get(0).getAttributes().get("url").toString());

		// Check bundle
		List<Capability> bundleCaps = resource.getCapabilities("osgi.wiring.bundle");
		assertEquals(1, bundleCaps.size());
		assertEquals("name.njbartlett.osgi.emf.minimal", bundleCaps.get(0).getAttributes().get("osgi.wiring.bundle"));
		assertEquals(new Version("2.7.0.201104130744"), bundleCaps.get(0).getAttributes().get("bundle-version"));

		// Check packages
		List<Capability> pkgCaps = resource.getCapabilities("osgi.wiring.package");
		assertNotNull(pkgCaps);
		assertEquals(14, pkgCaps.size());
		assertEquals("org.eclipse.emf.common", pkgCaps.get(0).getAttributes().get("osgi.wiring.package"));
		assertEquals(new Version("2.7.0.201104130744"), pkgCaps.get(0).getAttributes().get("version"));
		assertEquals("org.eclipse.core.runtime,org.eclipse.emf.common.util,org.osgi.framework",
				pkgCaps.get(0).getDirectives().get("uses"));

		// Check service capabilities of felix.shell bundle
		List<Capability> svcCaps = resources.get(4).getCapabilities("osgi.service");
		assertNotNull(svcCaps);
		assertEquals(2, svcCaps.size());
		assertEquals("org.apache.felix.shell.ShellService", svcCaps.get(0).getAttributes().get("osgi.service"));
		assertEquals("org.ungoverned.osgi.service.shell.ShellService",
				svcCaps.get(1).getAttributes().get("osgi.service"));
	}

	public static void testObrContentReqs() throws Exception {
		FileInputStream stream = new FileInputStream("testdata/fullobr.xml");
		URI baseUri = new File("testdata").toURI();
		List<Resource> resources = parseIndex(stream, baseUri);
		assertEquals(7, resources.size());

		// Check package imports of emf.minimal 2.7.0
		List<Requirement> pkgReqs = resources.get(0).getRequirements("osgi.wiring.package");
		assertNotNull(pkgReqs);
		assertEquals(20, pkgReqs.size());

		// Check mandatory/optional and filters
		assertNull(pkgReqs.get(0).getDirectives().get("resolution"));
		assertEquals("optional", pkgReqs.get(5).getDirectives().get("resolution"));
		assertEquals("(&(osgi.wiring.package=javax.crypto)(version>=0.0.0))",
				pkgReqs.get(0).getDirectives().get("filter"));

		// Check service requires of felix.shell
		List<Requirement> svcReqs = resources.get(4).getRequirements("osgi.service");
		assertNotNull(svcReqs);
		assertEquals(2, svcReqs.size());

		assertEquals("(osgi.service=org.osgi.service.startlevel.StartLevel)",
				svcReqs.get(0).getDirectives().get("filter"));
		assertEquals("(osgi.service=org.osgi.service.packageadmin.PackageAdmin)",
				svcReqs.get(1).getDirectives().get("filter"));
	}

	public static void testObrEEReq() throws Exception {
		FileInputStream stream = new FileInputStream("testdata/bree-obr.xml");
		URI baseUri = new File("testdata").toURI();
		List<Resource> resources = parseIndex(stream, baseUri);
		assertEquals(1, resources.size());

		List<Requirement> eeReqs = resources.get(0).getRequirements("osgi.ee");
		assertNotNull(eeReqs);
		assertEquals(1, eeReqs.size());

		assertEquals("(|(osgi.ee=J2SE-1.4)(osgi.ee=OSGi/Minimum-1.1))", eeReqs.get(0).getDirectives().get("filter"));
	}

}
