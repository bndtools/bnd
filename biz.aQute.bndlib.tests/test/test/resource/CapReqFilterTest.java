package test.resource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;

public class CapReqFilterTest {

	@Test
	public void testReqNoFilterDifferentNamespace() throws Exception {
		Capability cap = new CapabilityBuilder("run.amock").buildSyntheticCapability();
		Requirement req = new RequirementBuilder("bamboozled").buildSyntheticRequirement();

		assertFalse(ResourceUtils.matches(req, cap));
	}

	@Test
	public void testReqNoFilterCapWithAttr() throws Exception {
		Capability cap = new CapabilityBuilder("foobar").addAttribute("blah", "baz")
			.addAttribute("version", "42")
			.buildSyntheticCapability();
		Requirement req = new RequirementBuilder("foobar").buildSyntheticRequirement();

		assertTrue(ResourceUtils.matches(req, cap));
	}

	@Test
	public void testReqNoFilterCapWithNoAttr() {
		Capability cap = new CapabilityBuilder("foobar").buildSyntheticCapability();
		Requirement req = new RequirementBuilder("foobar").buildSyntheticRequirement();

		assertTrue(ResourceUtils.matches(req, cap));
	}

	@Test
	public void testReqWithFilter() {
		Capability cap = new CapabilityBuilder("foobar").buildSyntheticCapability();
		Requirement req = new RequirementBuilder("foobar").addDirective("filter", "(something=true)")
			.buildSyntheticRequirement();

		assertFalse(ResourceUtils.matches(req, cap));
	}

}
