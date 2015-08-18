package test.resource;

import aQute.bnd.osgi.resource.CapabilityBuilder;
import junit.framework.TestCase;

public class CapReqBuilderTest extends TestCase {

	public void testSimple() throws Exception {
		CapabilityBuilder cb = new CapabilityBuilder("test");
	}
}
