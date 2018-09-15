package test.resource;

import org.osgi.resource.Requirement;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import junit.framework.TestCase;

public class CapReqBuilderTest extends TestCase {

	public void testSimple() throws Exception {
		CapabilityBuilder cb = new CapabilityBuilder("test");
	}

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

	public void testAliasedRequirement() throws Exception {
		Parameters params = OSGiHeader.parseHeader("bnd.identity; id=org.example.foo");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);
		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(osgi.identity=org.example.foo)", req.getDirectives()
			.get("filter"));
	}

	public void testAliasedRequirementWithVersion() throws Exception {
		Parameters params = OSGiHeader.parseHeader("bnd.identity; id=org.example.foo; version=1.2");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);
		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(&(osgi.identity=org.example.foo)(version>=1.2.0))", req.getDirectives()
			.get("filter"));
	}

	public void testAliasedRequirementWithVersionRange() throws Exception {
		Parameters params = OSGiHeader.parseHeader("bnd.identity; id=org.example.foo; version='[1.2,1.3)'");
		Requirement req = CapReqBuilder.getRequirementsFrom(params)
			.get(0);

		assertEquals("osgi.identity", req.getNamespace());
		assertEquals("(&(osgi.identity=org.example.foo)(&(version>=1.2.0)(!(version>=1.3.0))))", req.getDirectives()
			.get("filter"));
	}

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

	public void testNonAliasedRequirementUnchanged() throws Exception {
		Parameters params = OSGiHeader.parseHeader("osgi.identity; filter:='(a=b)'; resolution:=optional");
		Requirement original = CapReqBuilder.getRequirementsFrom(params, false)
			.get(0);
		Requirement unaliased = CapReqBuilder.unalias(original);
		assertTrue("unaliasing a normal requirement should return the original object", original == unaliased);
	}

}
