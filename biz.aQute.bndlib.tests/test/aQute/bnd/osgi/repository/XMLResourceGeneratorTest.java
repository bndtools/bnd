package aQute.bnd.osgi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class XMLResourceGeneratorTest extends TestCase {
	private static final Requirement	WILDCARD	= ResourceUtils.createWildcardRequirement();
	File								tmp;

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	@Override
	protected void setUp() {
		tmp = IO.getFile("generated/tmp/test/" + getTestName());
		IO.delete(tmp);
		tmp.mkdirs();
	}

	public void testBasic() throws URISyntaxException, Exception {
		Repository repository = getTestRepository();
		File location = new File(tmp, "index.xml");
		new XMLResourceGenerator().name("test")
			.repository(repository)
			.save(location);

		Repository other = getRepository(location.toURI());
		Map<Requirement, Collection<Capability>> findProviders = other.findProviders(Collections.singleton(WILDCARD));

		Set<Resource> resources = ResourceUtils.getResources(findProviders.get(WILDCARD));
		assertEquals(1, resources.size());
		Resource r = resources.iterator()
			.next();
		assertThat(ResourceUtils.getContentCapability(r)
			.url()
			.toString()).endsWith("/name.njbartlett.eclipse.macbadge_1.0.0.201110100042.jar");
	}

	private Repository getTestRepository() throws URISyntaxException, Exception {
		return getRepository(XMLResourceGeneratorTest.class.getResource("data/macbadge.xml")
			.toURI());
	}

	private Repository getRepository(URI uri) throws IOException, Exception {
		try (XMLResourceParser p = new XMLResourceParser(uri);) {

			List<Resource> parse = p.parse();
			assertEquals(1, parse.size());
			return new ResourcesRepository(parse);
		}
	}

}
