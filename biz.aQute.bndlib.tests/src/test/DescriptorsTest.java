package test;

import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import junit.framework.TestCase;

public class DescriptorsTest extends TestCase {

	public static void testReferences() {
		Descriptors d = new Descriptors();
		TypeRef r = d.getTypeRef("[B");
		assertNotNull(r);
		assertEquals("byte[]", r.getFQN());
		assertNotNull(r.getPackageRef());
		assertEquals(".", r.getPackageRef()
			.getFQN());

		PackageRef a = d.getPackageRef("a.b.c");
		PackageRef b = d.getPackageRef("a/b/c");
		assertTrue(a == b);

	}
}
