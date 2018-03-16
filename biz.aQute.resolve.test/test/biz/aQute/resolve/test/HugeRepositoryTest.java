package biz.aQute.resolve.test;

import java.util.List;

import org.osgi.resource.Resource;

import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import biz.aQute.resolve.ResolverValidator;
import biz.aQute.resolve.ResolverValidator.Resolution;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class HugeRepositoryTest extends TestCase {

	public void testHugeWithSpecificResources() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_7, "system.bundle", "0.0.0");
			system.addManifest(OSGI_CORE.R6_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/collaboration-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/forms-and-workflow-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/foundation-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/target.platform.index.xml.gz")
				.toURI());
			List<Resource> resources = XMLResourceParser
				.getResources(IO.getFile("testdata/web-experience-1.0-index.xml.gz")
					.toURI());
			List<Resolution> resolutions = validator.validate(resources);
			assertTrue(validator.check());
			assertEquals(resources.size(), resolutions.size());
		}
	}

	public void testHugeWithSpecificResourcesAndDuplication() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder resourceBuilder = new ResourceBuilder();

			resourceBuilder.addEE(EE.JavaSE_1_7, "system.bundle", "0.0.0");
			resourceBuilder.addManifest(OSGI_CORE.R6_0_0.getManifest());
			validator.setSystem(resourceBuilder.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/collaboration-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/forms-and-workflow-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/foundation-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/target.platform.index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/web-experience-1.0-index.xml.gz")
				.toURI());
			List<Resource> resources = XMLResourceParser.getResources(IO.getFile("testdata/foundation-1.0-index.xml.gz")
				.toURI());
			List<Resolution> resolutions = validator.validate(resources);
			assertTrue(validator.check());
			assertEquals(resources.size(), resolutions.size());
		}
	}

}
