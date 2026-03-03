package aQute.bnd.osgi.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.version.Version;
import aQute.lib.collections.ExtList;

public class CapReqBuilderTest {


	@Test
	public void testSimple() throws Exception {
		CapabilityBuilder cb = new CapabilityBuilder("test");
	}

	@Test
	public void testParseRequirement() throws Exception {
		Parameters params = OSGiHeader.parseHeader("osgi.identity; filter:='(a=b)'; resolution:=optional");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);
		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("optional", req.getDirectives()
			.get("resolution"));
		assertEquals("(a=b)", req.getDirectives()
			.get("filter"));
	}

	@Test
	public void testAliasedRequirement() throws Exception {
		Parameters params = OSGiHeader.parseHeader("bnd.identity; id=org.example.foo");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);
		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(osgi.identity=org.example.foo)", req.getDirectives()
			.get("filter"));
	}

	@Test
	public void testAliasedRequirementWithVersion() throws Exception {
		Parameters params = OSGiHeader.parseHeader("bnd.identity; id=org.example.foo; version=1.2");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);
		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(&(osgi.identity=org.example.foo)(version>=1.2.0))", req.getDirectives()
			.get("filter"));
	}

	@Test
	public void testAliasedRequirementWithVersionRange() throws Exception {
		Parameters params = OSGiHeader.parseHeader("bnd.identity; id=org.example.foo; version='[1.2,1.3)'");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);

		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(&(osgi.identity=org.example.foo)(version>=1.2.0)(!(version>=1.3.0)))", req.getDirectives()
			.get("filter"));
	}

	@Test
	public void testAliasedFeatureRequirementWithTypeAndExactVersion() throws Exception {
		Parameters params = OSGiHeader.parseHeader(
			"bnd.identity; id=org.eclipse.rcp; type=org.eclipse.update.feature; version=4.37.0.v20250905-0730");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);

		assertEquals("osgi.identity", req.getNamespace());
		assertEquals(
			"(&(osgi.identity=org.eclipse.rcp)(type=org.eclipse.update.feature)(version>=4.37.0.v20250905-0730))",
			req.getDirectives()
				.get("filter"));
	}

	@Test
	public void testAliasedFeatureRequirementWithTypeAndVersionRange() throws Exception {
		Parameters params = OSGiHeader.parseHeader(
			"bnd.identity; id=org.eclipse.rcp; type=org.eclipse.update.feature; version='[4.37.0.v20250905-0730,4.38.0)'");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);

		assertEquals("osgi.identity", req.getNamespace());
		assertEquals(
			"(&(osgi.identity=org.eclipse.rcp)(type=org.eclipse.update.feature)(version>=4.37.0.v20250905-0730)(!(version>=4.38.0)))",
			req.getDirectives()
				.get("filter"));
	}

	@Test
	public void testCopyingAttributeWithVersionLists() throws Exception {
		Version b123 = new Version("1.2.3");
		Version b234 = new Version("2.3.4");
		Version b345 = new Version("3.4.5");
		org.osgi.framework.Version o123 = new org.osgi.framework.Version("1.2.3");
		org.osgi.framework.Version o234 = new org.osgi.framework.Version("2.3.4");
		org.osgi.framework.Version o345 = new org.osgi.framework.Version("3.4.5");
		Version[] barray = new Version[] {
			b123, b234, b345
		};
		org.osgi.framework.Version[] oarray = new org.osgi.framework.Version[] {
			o123, o234, o345
		};
		List<Version> blist = new ExtList<>(b123, b234, b345);
		List<org.osgi.framework.Version> olist = new ExtList<>(o123, o234, o345);

		Attrs attrs = new Attrs();
		attrs.putTyped("b123", b123);
		attrs.putTyped("o345", o345);

		attrs.putTyped("barray", barray);
		attrs.putTyped("oarray", oarray);

		attrs.putTyped("blist", blist);
		attrs.putTyped("olist", olist);

		CapReqBuilder cr = new CapReqBuilder("test");
		cr.addAttributes(attrs);
		CapabilityImpl cap = cr.buildSyntheticCapability();

		Map<String, Object> attributes = cap.getAttributes();

		assertThat(attributes.get("b123")).isInstanceOf(org.osgi.framework.Version.class)
			.isEqualTo(o123);
		assertThat(attributes.get("o345")).isInstanceOf(org.osgi.framework.Version.class)
			.isEqualTo(o345);

		assertThat(attributes.get("barray")).isEqualTo(olist);
		assertThat(attributes.get("oarray")).isEqualTo(olist);

		assertThat(attributes.get("blist")).isEqualTo(olist);
		assertThat(attributes.get("olist")).isEqualTo(olist);
	}

	@Test
	public void testAliasedRequirementCopyAttributesAndDirectives() throws Exception {
		Attrs attrs = new Attrs();
		attrs.putTyped("id", "org.example.foo");
		attrs.putTyped("size", 23L);
		attrs.put("resolution:", "optional");
		Requirement req = CapReqBuilder.getRequirementFrom("bnd.identity", attrs);

		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(osgi.identity=org.example.foo)", req.getDirectives()
			.get("filter"));
		assertEquals(23L, req.getAttributes()
			.get("size"));
		assertEquals("optional", req.getDirectives()
			.get("resolution"));
	}

	@Test
	public void testParseLiteralAliasedRequirement() throws Exception {
		Attrs attrs = new Attrs();
		attrs.putTyped("bnd.literal", "bnd.identity");
		attrs.putTyped("size", 23L);
		attrs.put("resolution:", "optional");
		attrs.put("filter:", "(bnd.identity=org.example.foo)");
		Requirement req = CapReqBuilder.getRequirementFrom("bnd.literal", attrs);

		assertEquals("bnd.identity", req.getNamespace());
		assertEquals("(bnd.identity=org.example.foo)", req.getDirectives()
			.get("filter"));
		assertEquals(23L, req.getAttributes()
			.get("size"));
		assertEquals("optional", req.getDirectives()
			.get("resolution"));
	}

	@Test
	public void testParseLiteralLiteralRequirement() throws Exception {
		Attrs attrs = new Attrs();
		attrs.putTyped("bnd.literal", "bnd.literal");
		attrs.putTyped("size", 23L);
		attrs.put("resolution:", "optional");
		attrs.put("filter:", "(bnd.literal=org.example.foo)");
		Requirement req = CapReqBuilder.getRequirementFrom("bnd.literal", attrs);

		assertEquals("bnd.literal", req.getNamespace());
		assertEquals("(bnd.literal=org.example.foo)", req.getDirectives()
			.get("filter"));
		assertEquals(23L, req.getAttributes()
			.get("size"));
		assertEquals("optional", req.getDirectives()
			.get("resolution"));
	}

	@Test
	public void testNonAliasedRequirementUnchanged() throws Exception {
		Parameters params = OSGiHeader.parseHeader("osgi.identity; filter:='(a=b)'; resolution:=optional");
		Requirement original = CapReqBuilder.getRequirementsFrom(params, false)
			.get(0);
		Requirement unaliased = CapReqBuilder.unalias(original);
		assertTrue(original == unaliased, "unaliasing a normal requirement should return the original object");
	}

	@Test
	public void testCapabilityToRequirementWithFilter() throws Exception {
		CapReqBuilder cr = new CapReqBuilder("osgi.wiring.package");
		Attrs attrs = new Attrs();
		attrs.putTyped("bundle-symbolic-name", "org.example");
		attrs.putTyped("bundle-version", "1.7.23");
		attrs.putTyped("osgi.wiring.package", "org.example.foo");
		attrs.putTyped("version", "1.7.23");
		attrs.putTyped("bnd.hashes", "123, 456, 789");
		cr.addAttributes(attrs);

		Capability cap = cr.buildSyntheticCapability();

		Requirement req = CapReqBuilder.createRequirementFromCapability(cap)
			.buildSyntheticRequirement();

		assertEquals("osgi.wiring.package", req.getNamespace());
		assertEquals(
			"(&(bundle-symbolic-name=org.example)(bundle-version>=1.7.23)(osgi.wiring.package=org.example.foo)(version>=1.7.23)(bnd.hashes=123, 456, 789))",
			req.getDirectives()
				.get("filter"));

		Requirement reqFiltered = CapReqBuilder.createRequirementFromCapability(cap, (name) -> {
			if (name.equals("bundle-symbolic-name") || name.equals("bundle-version") || name.equals("bnd.hashes")) {
				return false;
			}

			return true;
		})
			.buildSyntheticRequirement();

		assertEquals("osgi.wiring.package", reqFiltered.getNamespace());
		assertEquals("(&(osgi.wiring.package=org.example.foo)(version>=1.7.23))",
			reqFiltered.getDirectives()
			.get("filter"));
	}

	@Test
	public void testDetectDuplicateExports() throws Exception {

		// finding packages that have the same name but differ in their
		// class.
		// Sinces the "hashes" attribute contains the hashes of each class,
		// we'd see differences. The problem was that there were two
		// exporters with different packages with the same name.

		Capability cap1 = createCap("org.bundleA", "org.example.foo", "1.7.23", "123", "456", "789");

		// same package but differences in the hashes (missing one hash) -> this
		// is a problem
		Capability cap2 = createCap("org.bundleB", "org.example.foo", "1.7.23", "456", "789");

		// some other OK cap
		Capability cap3 = createCap("org.bundleC", "org.example.bar", "1.7.23", "789,910");


		List<Capability> culprits = ResourceUtils.detectDuplicateCapabilitiesWithDifferentHashes("osgi.wiring.package",
			Arrays.asList(cap1, cap2, cap3));

		System.out.println(
			"Culprits: the following capabilities provide the same package but with different package content");
		for (Capability c : culprits) {
			System.out.println(c);
		}

		assertEquals(2, culprits.size());
		assertThat(culprits).containsExactly(cap1, cap2);
	}



	private CapabilityImpl createCap(String bundleSymName, String pck, String version, String... hashes) {
		CapReqBuilder cr = new CapReqBuilder("osgi.wiring.package");
		Attrs attrs1 = new Attrs();
		attrs1.putTyped("bundle-symbolic-name", bundleSymName);
		attrs1.putTyped("osgi.wiring.package", pck);
		attrs1.putTyped("version", version);
		attrs1.putTyped("bnd.hashes", hashes);
		cr.addAttributes(attrs1);

		return cr.buildSyntheticCapability();
	}

}
