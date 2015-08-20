package test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Resource;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import junit.framework.TestCase;

/**
 * Test the repository classes
 */
public class RepositoryTest extends TestCase {

	public void testXMLParser() throws Exception {
		URL url = RepositoryTest.class.getResource("larger-repo.xml");
		try (XMLResourceParser xrp = new XMLResourceParser(url.toURI());) {
			xrp.setTrace(true);
			List<Resource> resources = xrp.parse();
			assertTrue(xrp.check());
			assertNotNull(resources);
			assertEquals(61, resources.size());
		}
	}

	public ResourcesRepository getResourcesRepository() throws Exception {
		List<Resource> resources = getResources();
		return new ResourcesRepository(resources);
	}

	private List<Resource> getResources() {
		List<Resource> l = new ArrayList<>();
		return null;
	}
}
