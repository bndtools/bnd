package test;

import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.MavenVersionRange;
import aQute.bnd.version.Version;
import junit.framework.TestCase;

public class MavenVersionTest extends TestCase {

	public void testRange() {
		MavenVersionRange mvr = new MavenVersionRange("[1.0.0,2.0.0)");
		assertEquals("[1.0.0,2.0.0)", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("2")));
	}

	public void testRangeWithOr() {
		MavenVersionRange mvr = new MavenVersionRange("[1.0.0  ,  2.0.0)  ,  [ 3.0.0, 4.0.0)");
		assertEquals("[1.0.0,2.0.0),[3.0.0,4.0.0)", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("2")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("3")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("4")));
	}

	public void testRangeWithLowExcludeAndHighInclude() {
		MavenVersionRange mvr = new MavenVersionRange("(1.0.0  ,  2.0.0]  ,  ( 3.0.0, 4.0.0]");
		assertEquals("(1.0.0,2.0.0],(3.0.0,4.0.0]", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("2")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("3")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("4")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("4.0.0.1")));
	}

	public void testCleanupWithMajor() {
		assertEquals("0.0.0.usedbypico", MavenVersion.cleanupVersion("usedbypico"));
		assertEquals("0.0.0.usedbypico", MavenVersion.cleanupVersion("use^%$#@dbypico"));
		assertEquals("0.0.0.usedbypico", MavenVersion.cleanupVersion("0.use^%$#@dbypico"));
	}
	public void testMajorMinorMicro() {
		MavenVersion mv = MavenVersion.parseString("1.2.3");
		assertEquals(new Version(1, 2, 3), mv.getOSGiVersion());
	}

	public void testMajorMinor() {
		MavenVersion mv = MavenVersion.parseString("1.2");
		assertEquals(new Version(1, 2), mv.getOSGiVersion());
	}

	public void testMajor() {
		MavenVersion mv = MavenVersion.parseString("1");
		assertEquals(new Version(1), mv.getOSGiVersion());
	}

	public void testSnapshot() {
		MavenVersion mv = MavenVersion.parseString("1.2.3-SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2-SNAPSHOT");
		assertEquals(new Version(1, 2, 0, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseString("1-SNAPSHOT");
		assertEquals(new Version(1, 0, 0, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2.3.SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2.3.BUILD-SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "BUILD-SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2-BUILD-SNAPSHOT");
		assertEquals(new Version(1, 2, 0, "BUILD-SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
	}

	public void testNumericQualifier() {
		MavenVersion mv = MavenVersion.parseString("1.2.3-01");
		assertEquals(new Version(1, 2, 3, "01"), mv.getOSGiVersion());
		mv = MavenVersion.parseString("1.2.3.01");
		assertEquals(new Version(1, 2, 3, "01"), mv.getOSGiVersion());
	}

	public void testQualifierWithDashSeparator() {
		MavenVersion mv = MavenVersion.parseString("1.2.3-beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testQualifierWithoutSeparator() {
		MavenVersion mv = MavenVersion.parseString("1.2.3rc1");
		assertEquals(new Version(1, 2, 3, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2rc1");
		assertEquals(new Version(1, 2, 0, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1rc1");
		assertEquals(new Version(1, 0, 0, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testQualifierWithDotSeparator() {
		MavenVersion mv = MavenVersion.parseString("1.2.3.beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2.beta-1");
		assertEquals(new Version(1, 2, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1.beta-1");
		assertEquals(new Version(1, 0, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testDotsInQualifier() {
		MavenVersion mv = MavenVersion.parseString("1.2.3.4.5");
		assertEquals(new Version(1, 2, 3, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2.3-4.5");
		assertEquals(new Version(1, 2, 3, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1.2-4.5");
		assertEquals(new Version(1, 2, 0, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("1-4.5");
		assertEquals(new Version(1, 0, 0, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testNull() {
		MavenVersion mv = MavenVersion.parseString(null);
		assertEquals(new Version(0, 0, 0), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testEmptyString() {
		MavenVersion mv = MavenVersion.parseString("");
		assertEquals(new Version(0, 0, 0), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseString("      	");
		assertEquals(new Version(0, 0, 0), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testInvalidVersion() {
		try {
			MavenVersion mv = MavenVersion.parseString("Not a number");
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testIsInteger() throws Exception {
		String max = String.valueOf(Integer.MAX_VALUE);
		String v = "1." + max + ".0";
		String c = MavenVersion.cleanupVersion(v);
		assertEquals(v, c);
		v = "1.0." + max;
		c = MavenVersion.cleanupVersion(v);
		assertEquals(v, c);
	}
}
