package biz.aQute.resolve.test;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;

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
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
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

			resourceBuilder.addEE(EE.JavaSE_1_8);
			resourceBuilder.addManifest(OSGI_CORE.R7_0_0.getManifest());
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

	public long testHugeValidateSelf() throws Exception {
		long start = System.nanoTime();
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/forms-and-workflow-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/foundation-1.0-index.xml.gz")
				.toURI());
			validator.addRepository(IO.getFile("testdata/target.platform.index.xml.gz")
				.toURI());
			validator.validate();
			return (System.nanoTime() - start) / 1000000000;
		}
	}

	public void testHugeValidateSelf1Iteration() throws Exception {
		List<Long> durations = new ArrayList<>();
		for (int i = 1; i <= 1; i++) {
			durations.add(testHugeValidateSelf());
		}

		LongSummaryStatistics summaryStatistics = durations.stream()
			.mapToLong(x -> x)
			.summaryStatistics();

		for (Long duration : durations) {
			System.out.printf("Duration: %ds%n", duration);
		}

		System.out.printf("Summary: %s%n", summaryStatistics);
	}

	public void testHugeValidateSelfIterations() throws Exception {
		List<Long> durations = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			durations.add(testHugeValidateSelf());
		}

		LongSummaryStatistics summaryStatistics = durations.stream()
			.mapToLong(x -> x)
			.summaryStatistics();

		for (Long duration : durations) {
			System.out.printf("Duration: %ds%n", duration);
		}

		System.out.printf("Summary: %s%n", summaryStatistics);
	}

}
