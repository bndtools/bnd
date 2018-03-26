package test;

import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		mvr = new MavenVersionRange("(1.0.0,2.0.0]");
		assertEquals("(1.0.0,2.0.0]", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("2")));
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

	public void testRangeWithEmptyLowerBound() {
		MavenVersionRange mvr = new MavenVersionRange("( , 1.0]");
		assertEquals("(,1.0]", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0.1")));
		mvr = new MavenVersionRange("( , 1.0 )");
		assertEquals("(,1.0)", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.9.9")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0")));
		mvr = new MavenVersionRange("[ , 1.0)");
		assertEquals("[,1.0)", mvr.toString());
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0.1")));
		mvr = new MavenVersionRange("[  , 1.0]");
		assertEquals("[,1.0]", mvr.toString());
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0.1")));
	}

	public void testRangeWithEmptyUpperBound() {
		MavenVersionRange mvr = new MavenVersionRange("( 1.0, ]");
		assertEquals("(1.0,]", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.1")));
		assertTrue(mvr.includes(MavenVersion.HIGHEST));
		mvr = new MavenVersionRange("(1.0,)");
		assertEquals("(1.0,)", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.1")));
		assertTrue(mvr.includes(MavenVersion.HIGHEST));
		mvr = new MavenVersionRange("[ 1.0, ]");
		assertEquals("[1.0,]", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0.9")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertTrue(mvr.includes(MavenVersion.HIGHEST));
		mvr = new MavenVersionRange("[1.0,)");
		assertEquals("[1.0,)", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0.9")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertTrue(mvr.includes(MavenVersion.HIGHEST));
	}

	public void testRangeSingle() {
		MavenVersionRange mvr = new MavenVersionRange("1.0");
		assertEquals("1.0", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0.9")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertTrue(mvr.includes(MavenVersion.HIGHEST));
	}

	public void testRangeExact() {
		MavenVersionRange mvr = new MavenVersionRange("[1.0]");
		assertEquals("[1.0]", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.parseMavenString("0.9")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.1")));
	}

	public void testCleanupWithMajor() {
		assertEquals("0.0.0.usedbypico", MavenVersion.cleanupVersion("usedbypico"));
		assertEquals("0.0.0.usedbypico", MavenVersion.cleanupVersion("use^%$#@dbypico"));
		assertEquals("0.0.0.usedbypico", MavenVersion.cleanupVersion("0.use^%$#@dbypico"));
	}

	public void testMajorMinorMicro() {
		MavenVersion mv = MavenVersion.parseString("1.2.3");
		assertEquals(new Version(1, 2, 3), mv.getOSGiVersion());
		mv = MavenVersion.parseString("1.0.2016062300");
		assertEquals(new Version(1, 0, 2016062300), mv.getOSGiVersion());
		mv = new MavenVersion("1.0.2016062300");
		assertEquals(new Version(1, 0, 2016062300), mv.getOSGiVersion());
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

	public void testComparableAliases() throws Exception {
		MavenVersion mv1 = MavenVersion.parseString("1.2.7");
		MavenVersion mv2 = MavenVersion.parseString("1.2.7-FINAL");
		MavenVersion mv3 = MavenVersion.parseString("1.2.7-final");
		MavenVersion mv4 = MavenVersion.parseString("1.2.7-GA");
		MavenVersion mv5 = MavenVersion.parseString("1.2.7-ga");
		assertEquals(0, mv1.compareTo(mv1));
		assertEquals(0, mv1.compareTo(mv2));
		assertEquals(0, mv1.compareTo(mv3));
		assertEquals(0, mv1.compareTo(mv4));
		assertEquals(0, mv1.compareTo(mv5));

		assertEquals(0, mv2.compareTo(mv1));
		assertEquals(0, mv2.compareTo(mv2));
		assertEquals(0, mv2.compareTo(mv3));
		assertEquals(0, mv2.compareTo(mv4));
		assertEquals(0, mv2.compareTo(mv5));

		assertEquals(0, mv3.compareTo(mv1));
		assertEquals(0, mv3.compareTo(mv2));
		assertEquals(0, mv3.compareTo(mv3));
		assertEquals(0, mv3.compareTo(mv4));
		assertEquals(0, mv3.compareTo(mv5));

		assertEquals(0, mv4.compareTo(mv1));
		assertEquals(0, mv4.compareTo(mv2));
		assertEquals(0, mv4.compareTo(mv3));
		assertEquals(0, mv4.compareTo(mv4));
		assertEquals(0, mv4.compareTo(mv5));

		assertEquals(0, mv5.compareTo(mv1));
		assertEquals(0, mv5.compareTo(mv2));
		assertEquals(0, mv5.compareTo(mv3));
		assertEquals(0, mv5.compareTo(mv4));
		assertEquals(0, mv5.compareTo(mv5));
	}

	public void testComparableSorting() throws Exception {
		List<String> ordered = Arrays.asList("1.2.7-ALPHA", "1.2.7-a2", "1.2.7-beta", "1.2.7-B50", "1.2.7-Milestone",
			"1.2.7-MILESTONE-2", "1.2.7-RC", "1.2.7-rc5", "1.2.7-SNAPSHOT", "1.2.7", "1.2.7-SP");
		List<String> shuffled = new ArrayList<>(ordered);
		Collections.shuffle(shuffled);
		assertNotEquals(ordered, shuffled);
		List<MavenVersion> sorted = shuffled.stream()
			.map(MavenVersion::new)
			.sorted()
			.collect(Collectors.toList());
		assertEquals(ordered.size(), sorted.size());
		for (int i = 0; i < ordered.size(); i++) {
			MavenVersion expected = MavenVersion.parseString(ordered.get(i));
			MavenVersion actual = sorted.get(i);
			assertEquals(expected, actual);
			assertEquals(0, expected.compareTo(actual));
			assertEquals(0, actual.compareTo(expected));
		}
	}

	public void testComparableMax() throws Exception {
		Optional<MavenVersion> m = Stream.of("1.2.7", "1.2.7-SNAPSHOT")
			.map(MavenVersion::parseString)
			.max(Comparator.naturalOrder());
		assertTrue(m.isPresent());
		assertEquals(new MavenVersion("1.2.7"), m.get());
	}
}
