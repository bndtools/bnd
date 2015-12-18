package test;

import aQute.bnd.version.VersionRange;
import junit.framework.TestCase;

public class VersionTest extends TestCase {

	public void testSimple() {

		compare("[0,1)", "[0.5,0.8]", "[0.5.0,0.8.0]");
		compare("[0,1)", "[0.5,0.8)", "[0.5.0,0.8.0)");
		compare("[0,1)", "[0.5,2]", "[0.5.0,1.0.0)");
	}

	void compare(String a, String b, String result) {
		assertEquals(result, new VersionRange(a).intersect(new VersionRange(b)).toString());
	}
}
