package test;

import junit.framework.*;
import aQute.bnd.osgi.*;

public class FilterTest extends TestCase {

	public void testFilter() throws Exception {
		String s = Verifier.validateFilter("(org.osgi.framework.windowing.system=xyz)");
		assertNull(s);
	}
}
