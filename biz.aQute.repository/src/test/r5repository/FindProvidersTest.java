package test.r5repository;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.lib.deployer.repository.FixedIndexedRepo;
import aQute.libg.version.VersionRange;
import biz.aQute.r5.resource.CapReqBuilder;

public class FindProvidersTest extends TestCase {

	public void testPackageQuery() throws Exception {
		/* TODO: disabled for now
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String,String> props = new HashMap<String,String>();
		props.put("locations", new File("testdata/minir5.xml").toURI().toString());
		repo.setProperties(props);
		
		Requirement req = CapReqBuilder.createPackageRequirement("org.example.a", new VersionRange("[1,2)")).buildSyntheticRequirement();
		Map<Requirement,Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
		
		assertNotNull(result);
		assertTrue(result.containsKey(req));
		Collection<Capability> caps = result.get(req);
		Capability[] capsArray = new Capability[1];
		capsArray = caps.toArray(capsArray );
		Capability identityCap = capsArray[0].getResource().getCapabilities("osgi.wiring.package").get(0);
		Object identityAttrValue = identityCap.getAttributes().get("osgi.identity");
		assertEquals("org.example.c", identityAttrValue);
		*/
	}
}
