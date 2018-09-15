package test;

import aQute.bnd.osgi.Verifier;
import junit.framework.TestCase;

public class FilterTest extends TestCase {

	public static void testFilter() throws Exception {
		String s = Verifier.validateFilter("(org.osgi.framework.windowing.system=xyz)");
		assertNull(s);
		s = Verifier.validateFilter("(&     (   org.osgi.framework.windowing.system   =xyz)     )");
		assertNull(s);
	}

}
