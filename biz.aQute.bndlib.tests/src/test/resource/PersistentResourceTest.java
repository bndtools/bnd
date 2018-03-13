package test.resource;

import java.util.List;

import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.PersistentResource;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.json.JSONCodec;
import junit.framework.TestCase;

public class PersistentResourceTest extends TestCase {

	public void testSimple() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addCapability(new CapReqBuilder("test").addAttribute("double", 3.0)
			.addAttribute("long", 3L)
			.addAttribute("string", "3.0")
			.addAttribute("version", new Version("3.0"))
			.buildSyntheticCapability());
		Resource r = rb.build();

		PersistentResource pr = new PersistentResource(r);
		String s = new JSONCodec().enc()
			.put(pr)
			.toString();

		PersistentResource pr2 = new JSONCodec().dec()
			.from(s)
			.get(PersistentResource.class);

		List<Capability> capabilities = pr.getResource()
			.getCapabilities(null);
		List<Requirement> requirements = pr.getResource()
			.getRequirements(null);
		assertEquals(1, capabilities.size());
		assertEquals(0, requirements.size());

		Capability capability = capabilities.get(0);
		assertEquals("test", capability.getNamespace());
		assertEquals(3.0, capability.getAttributes()
			.get("double"));
		assertEquals(3L, capability.getAttributes()
			.get("long"));
		assertEquals("3.0", capability.getAttributes()
			.get("string"));
		assertEquals(new Version("3.0"), capability.getAttributes()
			.get("version"));

		assertEquals(0, capability.getDirectives()
			.size());
	}
}
