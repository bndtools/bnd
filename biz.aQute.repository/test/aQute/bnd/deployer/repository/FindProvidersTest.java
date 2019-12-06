package aQute.bnd.deployer.repository;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class FindProvidersTest extends TestCase {

	public void testPackageQuery() throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Processor p = new Processor();
			p.addBasicPlugin(httpClient);
			repo.setRegistry(p);
			Map<String, String> props = new HashMap<>();
			props.put("locations", IO.getFile("testdata/minir5.xml")
				.toURI()
				.toString());
			props.put("name", getName());
			props.put("cache",
				new File("generated/tmp/test/cache/" + getClass().getName() + "/" + getName()).getAbsolutePath());
			repo.setProperties(props);

			Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", "[1,2)")
				.buildSyntheticRequirement();
			Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));

			assertNotNull(result);
			assertTrue(result.containsKey(req));
			Collection<Capability> caps = result.get(req);
			Capability[] capsArray = caps.toArray(new Capability[0]);
			Capability identityCap = capsArray[0].getResource()
				.getCapabilities("osgi.identity")
				.get(0);
			Object identityAttrValue = identityCap.getAttributes()
				.get("osgi.identity");
			assertEquals("dummybundle", identityAttrValue);
		}
	}

	public void testTypedCapabilityAttribute() throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Processor p = new Processor();
			p.addBasicPlugin(httpClient);
			repo.setRegistry(p);
			Map<String, String> props = new HashMap<>();
			props.put("locations", IO.getFile("testdata/minir5.xml")
				.toURI()
				.toString());
			props.put("name", getName());
			props.put("cache",
				new File("generated/tmp/test/cache/" + getClass().getName() + "/" + getName()).getAbsolutePath());
			repo.setProperties(props);

			Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", "[1,2)")
				.buildSyntheticRequirement();
			Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
			Capability id = result.get(req)
				.iterator()
				.next()
				.getResource()
				.getCapabilities("osgi.identity")
				.get(0);
			assertEquals(Version.class, id.getAttributes()
				.get("version")
				.getClass());
		}
	}

	public void testReadGZippedStream() throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Processor p = new Processor();
			p.addBasicPlugin(httpClient);
			repo.setRegistry(p);
			Map<String, String> props = new HashMap<>();
			props.put("locations", IO.getFile("testdata/big_index.xml.gz")
				.toURI()
				.toString());
			props.put("name", getName());
			props.put("cache",
				new File("generated/tmp/test/cache/" + getClass().getName() + "/" + getName()).getAbsolutePath());
			repo.setProperties(props);

			Requirement req = new CapReqBuilder("osgi.identity")
				.addDirective("filter", "(&(osgi.identity=osgi.cmpn)(version>=4.2.0)(!(version>=4.2.1)))")
				.buildSyntheticRequirement();
			Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));

			assertNotNull(result);
			assertTrue(result.containsKey(req));

			Collection<Capability> caps = result.get(req);
			assertEquals(1, caps.size());
			Capability identityCap = caps.iterator()
				.next();

			List<Capability> contentCaps = identityCap.getResource()
				.getCapabilities("osgi.content");
			assertNotNull(contentCaps);
			assertEquals(1, contentCaps.size());
			Capability contentCap = contentCaps.iterator()
				.next();
			assertEquals(IO.getFile("testdata/osgi.cmpn/osgi.cmpn-4.2.0.jar")
				.getAbsoluteFile()
				.toURI()
				.toString(),
				contentCap.getAttributes()
					.get("url")
					.toString());
		}
	}

	public void testMultipleMatches() throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Processor p = new Processor();
			p.addBasicPlugin(httpClient);
			repo.setRegistry(p);
			Map<String, String> props = new HashMap<>();
			props.put("locations", IO.getFile("testdata/big_index.xml")
				.toURI()
				.toString());
			props.put("name", getName());
			props.put("cache",
				new File("generated/tmp/test/cache/" + getClass().getName() + "/" + getName()).getAbsolutePath());
			repo.setProperties(props);

			Requirement req = CapReqBuilder.createPackageRequirement("aQute.bnd.annotation", "[1.43,2)")
				.buildSyntheticRequirement();
			Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
			Collection<Capability> matchingCaps = result.get(req);

			// 16 versions of biz.aQute.bndlib plus 8 versions of
			// biz.aQute.bnd.annotation!
			assertEquals(24, matchingCaps.size());

			for (Capability cap : matchingCaps) {
				Capability identityCap = cap.getResource()
					.getCapabilities("osgi.identity")
					.iterator()
					.next();
				String bsn = (String) identityCap.getAttributes()
					.get("osgi.identity");
				assertTrue("biz.aQute.bndlib".equals(bsn) || "biz.aQute.bnd.annotation".equals(bsn));
			}
		}
	}
}
