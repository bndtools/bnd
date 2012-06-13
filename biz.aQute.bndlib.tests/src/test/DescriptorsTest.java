package test;

import junit.framework.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.Descriptors.TypeRef;

public class DescriptorsTest extends TestCase {

	public void testReferences() {
		Descriptors d = new Descriptors();
		TypeRef r = d.getTypeRef("[B");
		assertNotNull(r);
		assertEquals("byte[]", r.getFQN());
		assertNotNull(r.getPackageRef());
		assertEquals(".", r.getPackageRef().getFQN());

		PackageRef a = d.getPackageRef("a.b.c");
		PackageRef b = d.getPackageRef("a/b/c");
		assertTrue(a == b);

	}
}
