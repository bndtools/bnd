package bndtools.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.CapabilityImpl;
import bndtools.model.resolution.CapReqComparator;
import bndtools.model.resolution.RequirementWrapper;

@ExtendWith(SoftAssertionsExtension.class)
public class AnalyseBundleResolutionJobTest {

	@Test
	public void testEmptyFileList() {
		AnalyseBundleResolutionJob job = new AnalyseBundleResolutionJob("resolve",
			Collections.<CapReqLoader> emptySet());

		IStatus status = job.run(new NullProgressMonitor());
		assertEquals(IStatus.OK, status.getCode());
		assertEquals(0, job.getCapabilities()
			.size());
		assertEquals(0, job.getRequirements()
			.size());
	}

	/*
	 * @Test public void testSimpleImport() { File[] files = new File[] { new
	 * File("test/tests.consumer.jar") }; AnalyseBundleResolutionJob job = new
	 * AnalyseBundleResolutionJob("resolve", files); IStatus status =
	 * job.run(new NullProgressMonitor()); assertEquals(IStatus.OK,
	 * status.getCode()); assertEquals(0, job.getExportResults().size());
	 * assertEquals(1, job.getImportResults().size()); ImportPackage apiImport =
	 * job.getImportResults().get(0); assertEquals("api", apiImport.getName());
	 * assertFalse(apiImport.isSelfImport()); }
	 */

	@Test
	public void testExportAndSelfImport() {
		AnalyseBundleResolutionJob job = new AnalyseBundleResolutionJob("resolve",
			Collections.singleton(new JarFileCapReqLoader(new File("test/tests.provider.jar"))));

		IStatus status = job.run(new NullProgressMonitor());
		assertEquals(IStatus.OK, status.getCode());

		Map<String, List<Capability>> caps = job.getCapabilities();
		assertEquals(4, caps.size());

		List<Capability> idCaps = caps.get("osgi.identity");
		assertEquals(1, idCaps.size());
		assertEquals("tests.provider", idCaps.get(0)
			.getAttributes()
			.get("osgi.identity"));
		assertEquals(new Version("0.0.0.201412192235"), idCaps.get(0)
			.getAttributes()
			.get("version"));

		List<Capability> bundleCaps = caps.get("osgi.wiring.bundle");
		assertEquals(1, bundleCaps.size());
		assertEquals("tests.provider", bundleCaps.get(0)
			.getAttributes()
			.get("osgi.wiring.bundle"));
		assertEquals(new Version("0.0.0.201412192235"), bundleCaps.get(0)
			.getAttributes()
			.get("bundle-version"));

		List<Capability> hostCaps = caps.get("osgi.wiring.host");
		assertEquals(1, hostCaps.size());
		assertEquals("tests.provider", hostCaps.get(0)
			.getAttributes()
			.get("osgi.wiring.host"));
		assertEquals(new Version("0.0.0.201412192235"), hostCaps.get(0)
			.getAttributes()
			.get("bundle-version"));

		List<Capability> exports = caps.get("osgi.wiring.package");
		assertEquals(1, exports.size());
		assertEquals("osgi.wiring.package", exports.get(0)
			.getNamespace());
		assertEquals("api", exports.get(0)
			.getAttributes()
			.get("osgi.wiring.package"));

		Map<String, List<RequirementWrapper>> reqs = job.getRequirements();
		assertEquals(2, reqs.size());

		List<RequirementWrapper> imports = reqs.get("osgi.wiring.package");
		assertEquals(1, imports.size());
		assertTrue(imports.get(0).resolved);
		assertEquals("(&(osgi.wiring.package=api)(version>=1.0.0)(!(version>=1.1.0)))",
			imports.get(0).requirement.getDirectives()
				.get("filter"));

		List<RequirementWrapper> ee = reqs.get("osgi.ee");
		assertEquals(1, ee.size());
		assertFalse(ee.get(0).resolved);
		assertEquals("(&(osgi.ee=JavaSE)(version=1.8))", ee.get(0).requirement.getDirectives()
			.get("filter"));

	}

	@Test
	public void testProvideCapability() {
		AnalyseBundleResolutionJob job = new AnalyseBundleResolutionJob("resolve",
			Collections.singleton(new JarFileCapReqLoader(new File("test/tests.consumer.jar"))));

		IStatus status = job.run(new NullProgressMonitor());
		assertEquals(IStatus.OK, status.getCode());
		List<Capability> caps = job.getCapabilities()
			.get("com.acme.display");
		assertEquals(1, caps.size());
		assertEquals(1024L, caps.get(0)
			.getAttributes()
			.get("width"));
		assertEquals(768L, caps.get(0)
			.getAttributes()
			.get("height"));
		assertEquals("com.acme", caps.get(0)
			.getDirectives()
			.get("uses"));
	}

	@Test
	public void testCapReqComparatorVersionList(SoftAssertions softly) {

		// test an aspect of ResolutionView which uses CapReqComparator

		// the following Provide-Capability header threw a
		// ClassCastException: class java.util.ArrayList cannot be cast to class
		// org.osgi.framework.Version
		// in a CapReqComparator
		// This test ensures that this exception does not occur
		// Provide-Capability: osgi.ee;osgi.ee="OSGi/Minimum";
		// version:List<Version>="1.0,1.1,1.2",osgi.ee;osgi.ee="JRE";
		// version:List<Version>="1.0,1.1,1.2,1.3"

		Comparator<Object> comparator = new CapReqComparator();

		Attrs attrs = new Attrs();
		attrs.put("version", Type.VERSIONS, "1.0,1.1,1.2,1.3");
		CapabilityImpl c1 = new CapabilityBuilder("osgi.ee").addAttributes(attrs)
			.buildSyntheticCapability();

		Attrs attrs2 = new Attrs();
		attrs2.put("version", Type.VERSIONS, "1.0,1.1,1.2");
		CapabilityImpl c2 = new CapabilityBuilder("osgi.ee").addAttributes(attrs2)
			.buildSyntheticCapability();

		// expect sorting by highest version

		softly.assertThat(comparator.compare(c1, c2))
			.isEqualTo(1);

		CapabilityImpl[] array = {
			c1, c2
		};

		softly.assertThat(Arrays.toString(array))
			.as("before sorting, highest version first")
			.isEqualTo(
				"[osgi.ee;version:List<Version>='1.0.0,1.1.0,1.2.0,1.3.0', osgi.ee;version:List<Version>='1.0.0,1.1.0,1.2.0']");
		Arrays.sort(array, comparator);
		softly.assertThat(Arrays.toString(array))
			.as("after sorting, highest version last")
			.isEqualTo(
				"[osgi.ee;version:List<Version>='1.0.0,1.1.0,1.2.0', osgi.ee;version:List<Version>='1.0.0,1.1.0,1.2.0,1.3.0']");

	}

}
