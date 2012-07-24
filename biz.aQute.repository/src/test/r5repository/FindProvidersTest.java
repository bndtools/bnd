package test.r5repository;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.resource.*;

import aQute.bnd.deployer.repository.*;
import aQute.bnd.osgi.resource.*;

public class FindProvidersTest extends TestCase {

	public void testPackageQuery() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", new File("testdata/minir5.xml").toURI().toString());
		repo.setProperties(props);
		
		Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", "[1,2)").buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
		
		assertNotNull(result);
		assertTrue(result.containsKey(req));
		Collection<Capability> caps = result.get(req);
		Capability[] capsArray = new Capability[1];
		capsArray = caps.toArray(capsArray );
		Capability identityCap = capsArray[0].getResource().getCapabilities("osgi.identity").get(0);
		Object identityAttrValue = identityCap.getAttributes().get("osgi.identity");
		assertEquals("dummybundle", identityAttrValue);
	}
	
	public void testTypedCapabilityAttribute() {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", new File("testdata/minir5.xml").toURI().toString());
		repo.setProperties(props);
		
		Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", "[1,2)").buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
		Capability id = result.get(req).iterator().next().getResource().getCapabilities("osgi.identity").get(0);
		assertEquals(Version.class, id.getAttributes().get("version").getClass());
	}

	public void testReadGZippedStream() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", new File("testdata/big_index.xml.gz").toURI().toString());
		repo.setProperties(props);
		
		Requirement req = new CapReqBuilder("osgi.identity").addDirective("filter", "(&(osgi.identity=osgi.cmpn)(version>=4.2.0)(!(version>=4.2.1)))").buildSyntheticRequirement();
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
		assertEquals(new File("testdata/osgi.cmpn/osgi.cmpn-4.2.0.jar").getAbsoluteFile().toURI(), contentCap.getAttributes().get("url"));
	}

	public void testMultipleMatches() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", new File("testdata/big_index.xml").toURI().toString());
		repo.setProperties(props);
		
		Requirement req = CapReqBuilder.createPackageRequirement("aQute.bnd.annotation", "[1.43,2)").buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
		Collection<Capability> matchingCaps = result.get(req);
		
		// 16 versions of biz.aQute.bndlib plus 8 versions of biz.aQute.bnd.annotation!
		assertEquals(24, matchingCaps.size());
		
		for (Capability cap : matchingCaps) {
			Capability identityCap = cap.getResource().getCapabilities("osgi.identity").iterator().next();
			String bsn = (String) identityCap.getAttributes().get("osgi.identity");
			assertTrue("biz.aQute.bndlib".equals(bsn) || "biz.aQute.bnd.annotation".equals(bsn));
		}
	}

}
