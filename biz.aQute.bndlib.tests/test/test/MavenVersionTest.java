package test;

import static org.assertj.core.api.Assertions.assertThat;
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
		assertFalse(mvr.includes(MavenVersion.LOWEST));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0.1")));
		mvr = new MavenVersionRange("( , 1.0 )");
		assertEquals("(,1.0)", mvr.toString());
		assertFalse(mvr.includes(MavenVersion.LOWEST));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.9.9")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0")));
		mvr = new MavenVersionRange("[ , 1.0)");
		assertEquals("[,1.0)", mvr.toString());
		assertTrue(mvr.includes(MavenVersion.LOWEST));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0")));
		assertTrue(mvr.includes(MavenVersion.parseMavenString("0.1")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0")));
		assertFalse(mvr.includes(MavenVersion.parseMavenString("1.0.1")));
		mvr = new MavenVersionRange("[  , 1.0]");
		assertEquals("[,1.0]", mvr.toString());
		assertTrue(mvr.includes(MavenVersion.LOWEST));
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
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3");
		assertEquals(new Version(1, 2, 3), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.0.2016062300");
		assertEquals(new Version(1, 0, 2016062300), mv.getOSGiVersion());
	}

	public void testMajorMinor() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2");
		assertEquals(new Version(1, 2), mv.getOSGiVersion());
	}

	public void testMajor() {
		MavenVersion mv = MavenVersion.parseMavenString("1");
		assertEquals(new Version(1), mv.getOSGiVersion());
	}

	public void testSnapshot() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3-SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2-SNAPSHOT");
		assertEquals(new Version(1, 2, 0, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1-SNAPSHOT");
		assertEquals(new Version(1, 0, 0, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2.3.SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2.3.BUILD-SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "BUILD-SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2-BUILD-SNAPSHOT");
		assertEquals(new Version(1, 2, 0, "BUILD-SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
	}

	public void testNumericQualifier() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3-01");
		assertEquals(new Version(1, 2, 3, "01"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.2.3.01");
		assertEquals(new Version(1, 2, 3, "01"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.2.3.201209091230120");
		assertEquals(new Version(1, 2, 3, "201209091230120"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.2.3.201209091230120-5");
		assertEquals(new Version(1, 2, 3, "201209091230120-5"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.2.201209091230120");
		assertEquals(new Version(1, 2, 0, "201209091230120"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.2.201209091230120.4-5");
		assertEquals(new Version(1, 2, 0, "2012090912301204-5"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.201209091230120");
		assertEquals(new Version(1, 0, 0, "201209091230120"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("1.201209091230120.3.4-5");
		assertEquals(new Version(1, 0, 0, "20120909123012034-5"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("201209091230120");
		assertEquals(new Version(0, 0, 0, "201209091230120"), mv.getOSGiVersion());
		mv = MavenVersion.parseMavenString("201209091230120.2.3.4-5");
		assertEquals(new Version(0, 0, 0, "201209091230120234-5"), mv.getOSGiVersion());
	}

	public void testQualifierWithDashSeparator() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3-beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testQualifierWithoutSeparator() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3rc1");
		assertEquals(new Version(1, 2, 3, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2rc1");
		assertEquals(new Version(1, 2, 0, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1rc1");
		assertEquals(new Version(1, 0, 0, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testQualifierWithDotSeparator() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3.beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2.beta-1");
		assertEquals(new Version(1, 2, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.beta-1");
		assertEquals(new Version(1, 0, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testDotsInQualifier() {
		MavenVersion mv = MavenVersion.parseMavenString("1.2.3.4.5");
		assertEquals(new Version(1, 2, 3, "45"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2.3-4.5");
		assertEquals(new Version(1, 2, 3, "45"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1.2-4.5");
		assertEquals(new Version(1, 2, 0, "45"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("1-4.5");
		assertEquals(new Version(1, 0, 0, "45"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testNull() {
		MavenVersion mv = MavenVersion.parseMavenString(null);
		assertEquals(new Version(0, 0, 0), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testEmptyString() {
		MavenVersion mv = MavenVersion.parseMavenString("");
		assertEquals(new Version(0, 0, 0), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MavenVersion.parseMavenString("      	");
		assertEquals(new Version(0, 0, 0), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testInvalidVersion() {
		MavenVersion mv = MavenVersion.parseMavenString("Not a number");
		assertEquals(new Version(0, 0, 0, "Notanumber"), mv.getOSGiVersion());
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
		MavenVersion mv1 = MavenVersion.parseMavenString("1.2.7");
		MavenVersion mv2 = MavenVersion.parseMavenString("1.2.7-FINAL");
		MavenVersion mv3 = MavenVersion.parseMavenString("1.2.7-final");
		MavenVersion mv4 = MavenVersion.parseMavenString("1.2.7-GA");
		MavenVersion mv5 = MavenVersion.parseMavenString("1.2.7-ga");
		MavenVersion mv6 = MavenVersion.parseMavenString("1.2.7-release");
		MavenVersion mv7 = MavenVersion.parseMavenString("1.2.7-RELEASE");
		assertThat(mv1).isEqualByComparingTo(mv2)
			.isEqualByComparingTo(mv3)
			.isEqualByComparingTo(mv4)
			.isEqualByComparingTo(mv5)
			.isEqualByComparingTo(mv6)
			.isEqualByComparingTo(mv7);
		assertThat(mv2).isEqualByComparingTo(mv1)
			.isEqualByComparingTo(mv3)
			.isEqualByComparingTo(mv4)
			.isEqualByComparingTo(mv5)
			.isEqualByComparingTo(mv6)
			.isEqualByComparingTo(mv7);
		assertThat(mv3).isEqualByComparingTo(mv1)
			.isEqualByComparingTo(mv2)
			.isEqualByComparingTo(mv4)
			.isEqualByComparingTo(mv5)
			.isEqualByComparingTo(mv6)
			.isEqualByComparingTo(mv7);
		assertThat(mv4).isEqualByComparingTo(mv1)
			.isEqualByComparingTo(mv2)
			.isEqualByComparingTo(mv3)
			.isEqualByComparingTo(mv5)
			.isEqualByComparingTo(mv6)
			.isEqualByComparingTo(mv7);
		assertThat(mv5).isEqualByComparingTo(mv1)
			.isEqualByComparingTo(mv2)
			.isEqualByComparingTo(mv3)
			.isEqualByComparingTo(mv4)
			.isEqualByComparingTo(mv6)
			.isEqualByComparingTo(mv7);
		assertThat(mv6).isEqualByComparingTo(mv1)
			.isEqualByComparingTo(mv2)
			.isEqualByComparingTo(mv3)
			.isEqualByComparingTo(mv4)
			.isEqualByComparingTo(mv5)
			.isEqualByComparingTo(mv7);
		assertThat(mv7).isEqualByComparingTo(mv1)
			.isEqualByComparingTo(mv2)
			.isEqualByComparingTo(mv3)
			.isEqualByComparingTo(mv4)
			.isEqualByComparingTo(mv5)
			.isEqualByComparingTo(mv6);
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
			MavenVersion expected = MavenVersion.parseMavenString(ordered.get(i));
			MavenVersion actual = sorted.get(i);
			assertEquals(expected, actual);
			assertEquals(0, expected.compareTo(actual));
			assertEquals(0, actual.compareTo(expected));
		}
	}

	public void testComparableMax() throws Exception {
		Optional<MavenVersion> m = Stream.of("1.2.7", "1.2.7-SNAPSHOT")
			.map(MavenVersion::parseMavenString)
			.max(Comparator.naturalOrder());
		assertTrue(m.isPresent());
		assertEquals(new MavenVersion("1.2.7"), m.get());
	}

	public void testJavaFXVersion() throws Exception {
		MavenVersion mv = MavenVersion.parseMavenString("13-ea+8");
		assertEquals(new Version(13, 0, 0, "ea8"), mv.getOSGiVersion());
	}

	// See https://maven.apache.org/pom.html#Version_Order_Specification
	public void testMavenVersionOrder() {
		MavenVersion mv1, mv2, mv3, mv4, mv5, mv6, mv7;

		mv1 = MavenVersion.parseMavenString("1-1.foo-bar1baz-.1");
		mv2 = MavenVersion.parseMavenString("1-1.foo-bar-1-baz-0.1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.0.0");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.ga");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.final");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.0");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1-");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.0.0-foo.0.0");
		mv2 = MavenVersion.parseMavenString("1-foo");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1.0.0-0.0.0");
		mv2 = MavenVersion.parseMavenString("1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString("1");
		mv2 = MavenVersion.parseMavenString("1.1");
		assertThat(mv1).isLessThan(mv2);

		mv1 = MavenVersion.parseMavenString("1-snapshot");
		mv2 = MavenVersion.parseMavenString("1");
		mv3 = MavenVersion.parseMavenString("1-sp");
		assertThat(mv1).isLessThan(mv2)
			.isLessThan(mv3);
		assertThat(mv2).isLessThan(mv3);

		mv1 = MavenVersion.parseMavenString("1-foo2");
		mv2 = MavenVersion.parseMavenString("1-foo10");
		assertThat(mv1).isLessThan(mv2);

		mv1 = MavenVersion.parseMavenString("1.foo");
		mv2 = MavenVersion.parseMavenString("1-foo");
		mv3 = MavenVersion.parseMavenString("1-1");
		mv4 = MavenVersion.parseMavenString("1.1");
		assertThat(mv1).isLessThan(mv2)
			.isLessThan(mv3)
			.isLessThan(mv4);
		assertThat(mv2).isLessThan(mv3)
			.isLessThan(mv4);
		assertThat(mv3).isLessThan(mv4);

		mv1 = MavenVersion.parseMavenString("1.ga");
		mv2 = MavenVersion.parseMavenString("1-ga");
		mv3 = MavenVersion.parseMavenString("1-0");
		mv4 = MavenVersion.parseMavenString("1.0");
		mv5 = MavenVersion.parseMavenString("1");
		mv6 = MavenVersion.parseMavenString("1.release");
		mv7 = MavenVersion.parseMavenString("1-release");
		assertThat(mv1).isEqualTo(mv2)
			.isEqualTo(mv3)
			.isEqualTo(mv4)
			.isEqualTo(mv5)
			.isEqualTo(mv6)
			.isEqualTo(mv7);
		assertThat(mv2).isEqualTo(mv1)
			.isEqualTo(mv3)
			.isEqualTo(mv4)
			.isEqualTo(mv5)
			.isEqualTo(mv6)
			.isEqualTo(mv7);
		assertThat(mv3).isEqualTo(mv1)
			.isEqualTo(mv2)
			.isEqualTo(mv4)
			.isEqualTo(mv5)
			.isEqualTo(mv6)
			.isEqualTo(mv7);
		assertThat(mv4).isEqualTo(mv1)
			.isEqualTo(mv2)
			.isEqualTo(mv3)
			.isEqualTo(mv5)
			.isEqualTo(mv6)
			.isEqualTo(mv7);
		assertThat(mv5).isEqualTo(mv1)
			.isEqualTo(mv2)
			.isEqualTo(mv3)
			.isEqualTo(mv4)
			.isEqualTo(mv6)
			.isEqualTo(mv7);
		assertThat(mv6).isEqualTo(mv1)
			.isEqualTo(mv2)
			.isEqualTo(mv3)
			.isEqualTo(mv4)
			.isEqualTo(mv5)
			.isEqualTo(mv7);
		assertThat(mv7).isEqualTo(mv1)
			.isEqualTo(mv2)
			.isEqualTo(mv3)
			.isEqualTo(mv4)
			.isEqualTo(mv5)
			.isEqualTo(mv6);

		mv1 = MavenVersion.parseMavenString("1-sp");
		mv2 = MavenVersion.parseMavenString("1-ga");
		assertThat(mv1).isGreaterThan(mv2);

		mv1 = MavenVersion.parseMavenString("1-sp.1");
		mv2 = MavenVersion.parseMavenString("1-ga.1");
		assertThat(mv1).isGreaterThan(mv2);

		mv1 = MavenVersion.parseMavenString("1-sp-1");
		mv2 = MavenVersion.parseMavenString("1-ga-1");
		mv3 = MavenVersion.parseMavenString("1-1");
		assertThat(mv1).isLessThan(mv2)
			.isLessThan(mv3);

		mv1 = MavenVersion.parseMavenString("1-a1");
		mv2 = MavenVersion.parseMavenString("1-alpha-1");
		assertThat(mv1).isEqualTo(mv2);

		mv1 = MavenVersion.parseMavenString(null);
		mv2 = MavenVersion.LOWEST;
		assertThat(mv1).isGreaterThan(mv2);
	}

}
