package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;

/**
 * Test the repository classes
 */
public class RepositoryTest {

	@Test
	public void testXMLParserLarge() throws Exception {
		URL url = RepositoryTest.class.getResource("larger-repo.xml");
		try (XMLResourceParser xrp = new XMLResourceParser(url.toURI());) {
			xrp.setTrace(true);
			List<Resource> resources = xrp.parse();
			assertTrue(xrp.check());
			assertNotNull(resources);
			assertEquals(61, resources.size());

			Requirement requirement = new RequirementBuilder("osgi.extender")
				.addDirective("filter", "(osgi.extender=osgi.component)")
				.buildSyntheticRequirement();

			Map<Requirement, Collection<Capability>> caps = getResourcesRepository(resources)
				.findProviders(Collections.singleton(requirement));

			assertEquals(1, caps.get(requirement)
				.size());

			Resource res = caps.get(requirement)
				.iterator()
				.next()
				.getResource();

			assertEquals("org.apache.felix.scr", ResourceUtils.getIdentityCapability(res)
				.getAttributes()
				.get("osgi.identity"));

			String location = ResourceUtils.getContentCapability(res)
				.getAttributes()
				.get("url")
				.toString();
			String base = url.toURI()
				.toString();
			assertFalse(location.startsWith(base), location);
		}
	}

	@Test
	public void testXMLParserSmall() throws Exception {
		URL url = RepositoryTest.class.getResource("repoindex-file.xml");
		try (XMLResourceParser xrp = new XMLResourceParser(url.toURI());) {
			xrp.setTrace(true);
			List<Resource> resources = xrp.parse();
			assertTrue(xrp.check());
			assertNotNull(resources);
			assertEquals(21, resources.size());

			Requirement requirement = new RequirementBuilder("osgi.extender")
				.addDirective("filter", "(osgi.extender=osgi.component)")
				.buildSyntheticRequirement();

			Map<Requirement, Collection<Capability>> caps = getResourcesRepository(resources)
				.findProviders(Collections.singleton(requirement));

			assertEquals(1, caps.get(requirement)
				.size());

			Resource res = caps.get(requirement)
				.iterator()
				.next()
				.getResource();

			assertEquals("org.apache.felix.scr", ResourceUtils.getIdentityCapability(res)
				.getAttributes()
				.get("osgi.identity"));

			String location = ResourceUtils.getContentCapability(res)
				.getAttributes()
				.get("url")
				.toString();
			assertFalse(location.contains("file:"), location);
		}
	}

	public ResourcesRepository getResourcesRepository(List<Resource> resources) throws Exception {
		return new ResourcesRepository(resources);
	}

}
