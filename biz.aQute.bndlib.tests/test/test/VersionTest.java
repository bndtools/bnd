package test;

import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import junit.framework.TestCase;

public class VersionTest extends TestCase {

	public void testSimple() {

		compare("[0,1)", "[0.5,0.8]", "[0.5.0,0.8.0]");
		compare("[0,1)", "[0.5,0.8)", "[0.5.0,0.8.0)");
		compare("[0,1)", "[0.5,2]", "[0.5.0,1.0.0)");
	}

	public void testSnapshot() {
		assertFalse(new Version(1, 2, 3, null).isSnapshot());
		assertFalse(new Version(1, 2, 3, "").isSnapshot());
		assertFalse(new Version(1, 2, 3, "SNAPSHOTX").isSnapshot());
		assertFalse(new Version(1, 2, 3, "-SNAPSHOTX").isSnapshot());
		assertFalse(new Version(1, 2, 3, "snapshot").isSnapshot());
		assertFalse(new Version(1, 2, 3, "-snapshot").isSnapshot());
		assertFalse(new Version(1, 2, 3, "foo").isSnapshot());
		assertFalse(new Version(1, 2, 3, "snapshot-").isSnapshot());

		assertTrue(new Version(1, 2, 3, "SNAPSHOT").isSnapshot());
		assertTrue(new Version(1, 2, 3, "-SNAPSHOT").isSnapshot());
		assertTrue(new Version(1, 2, 3, "FOO-SNAPSHOT").isSnapshot());
		assertTrue(new Version(1, 2, 3, "20124121212-SNAPSHOT").isSnapshot());
		assertTrue(new Version("1.2.3.20124121212-SNAPSHOT").isSnapshot());
		assertTrue(new Version("1.2.3.-SNAPSHOT").isSnapshot());
		assertTrue(new Version("1.2.3.SNAPSHOT").isSnapshot());
	}

	void compare(String a, String b, String result) {
		assertEquals(result, new VersionRange(a).intersect(new VersionRange(b))
			.toString());
	}
}
