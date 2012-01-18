package test;

import junit.framework.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;

public class DescriptorsTest extends TestCase {

	public void testReferences() {
		Descriptors d = new Descriptors();
		
		PackageRef a = d.getPackageRef("a.b.c");
		PackageRef b = d.getPackageRef("a/b/c");
		assertTrue( a == b);
		
	}
}
