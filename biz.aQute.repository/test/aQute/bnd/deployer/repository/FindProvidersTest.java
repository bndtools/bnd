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

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class FindProvidersTest extends TestCase {

	public static void testPackageQuery() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/minir5.xml").toURI().toString());
		repo.setProperties(props);

		Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", "[1,2)").buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));

		assertNotNull(result);
		assertTrue(result.containsKey(req));
		Collection<Capability> caps = result.get(req);
		Capability[] capsArray = new Capability[1];
		capsArray = caps.toArray(capsArray);
		Capability identityCap = capsArray[0].getResource().getCapabilities("osgi.identity").get(0);
		Object identityAttrValue = identityCap.getAttributes().get("osgi.identity");
		assertEquals("dummybundle", identityAttrValue);
	}

	public static void testTypedCapabilityAttribute() {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/minir5.xml").toURI().toString());
		repo.setProperties(props);

		Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", "[1,2)").buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
		Capability id = result.get(req).iterator().next().getResource().getCapabilities("osgi.identity").get(0);
		assertEquals(Version.class, id.getAttributes().get("version").getClass());
	}

	public static void testReadGZippedStream() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/big_index.xml.gz").toURI().toString());
		repo.setProperties(props);

		Requirement req = new CapReqBuilder("osgi.identity")
				.addDirective("filter", "(&(osgi.identity=osgi.cmpn)(version>=4.2.0)(!(version>=4.2.1)))")
				.buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));

		assertNotNull(result);
		assertTrue(result.containsKey(req));

		Collection<Capability> caps = result.get(req);
		assertEquals(1, caps.size());
		Capability identityCap = caps.iterator().next();

		List<Capability> contentCaps = identityCap.getResource().getCapabilities("osgi.content");
		assertNotNull(contentCaps);
		assertEquals(1, contentCaps.size());
		Capability contentCap = contentCaps.iterator().next();
		assertEquals(IO.getFile("testdata/osgi.cmpn/osgi.cmpn-4.2.0.jar").getAbsoluteFile().toURI(),
				contentCap.getAttributes().get("url"));
	}

	public static void testMultipleMatches() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/big_index.xml").toURI().toString());
		repo.setProperties(props);

		Requirement req = CapReqBuilder.createPackageRequirement("aQute.bnd.annotation", "[1.43,2)")
				.buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
		Collection<Capability> matchingCaps = result.get(req);

		// 16 versions of biz.aQute.bndlib plus 8 versions of
		// biz.aQute.bnd.annotation!
		assertEquals(24, matchingCaps.size());

		for (Capability cap : matchingCaps) {
			Capability identityCap = cap.getResource().getCapabilities("osgi.identity").iterator().next();
			String bsn = (String) identityCap.getAttributes().get("osgi.identity");
			assertTrue("biz.aQute.bndlib".equals(bsn) || "biz.aQute.bnd.annotation".equals(bsn));
		}
	}

	public void testResolveByHash() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/big_index.xml").toURI().toString());
		repo.setProperties(props);

		Map<String,String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", "292ecca5d8fdb881f0273c1166b9c20925be063d3baf3824515983f76b73fffb");

		File bundle = repo.get("biz.aQute.bnd", null, searchProps);
		assertNotNull(bundle);
		// the bundle doesn't actually exist... just check the filename
		assertEquals("biz.aQute.bnd-1.45.0.jar", bundle.getName());
	}

	public void testResolveByHashWithAlgorithm() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/big_index.xml").toURI().toString());
		repo.setProperties(props);

		Map<String,String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", "SHA-256:292ecca5d8fdb881f0273c1166b9c20925be063d3baf3824515983f76b73fffb");
		File bundle = repo.get("biz.aQute.bnd", null, searchProps);
		assertNotNull(bundle);
		// the bundle doesn't actually exist... just check the filename
		assertEquals("biz.aQute.bnd-1.45.0.jar", bundle.getName());
	}

	public void testResolveByHashWithWrongAlgorithm() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/big_index.xml").toURI().toString());
		repo.setProperties(props);

		Map<String,String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", "SHA-1:292ecca5d8fdb881f0273c1166b9c20925be063d3baf3824515983f76b73fffb");
		File bundle = repo.get("biz.aQute.bnd", null, searchProps);
		assertNull(bundle);
	}

	public void testResolveByHashWithIdCheck() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", IO.getFile("testdata/big_index.xml").toURI().toString());
		repo.setProperties(props);

		Map<String,String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", "SHA-256:292ecca5d8fdb881f0273c1166b9c20925be063d3baf3824515983f76b73fffb");
		try {
			File bundle = repo.get("wrong.bsn", null, searchProps);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

}
