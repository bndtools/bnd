package test.resource;

import java.util.Arrays;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import junit.framework.TestCase;

public class CapReqBuilderTest extends TestCase {

	public void testSimple() throws Exception {
		CapabilityBuilder cb = new CapabilityBuilder("test");
		cb.addAttribute("version", new org.osgi.framework.Version("1.2.0"));
		cb.fixup();
		Attrs a = cb.toAttrs();
		assertEquals(Type.VERSION, a.getType("version"));

		cb.addAttribute("version", new aQute.bnd.version.Version("1.3.0"));
		cb.fixup();
		a = cb.toAttrs();
		assertEquals(Type.VERSION, a.getType("version"));

		cb.addAttribute("version", "1.3.0");
		cb.fixup();
		a = cb.toAttrs();
		assertEquals(Type.VERSION, a.getType("version"));

		cb.addAttribute("version", Arrays.asList(new org.osgi.framework.Version("1.3.0")));
		a = cb.toAttrs();
		assertEquals(Type.VERSIONS, a.getType("version"));
	}
}
