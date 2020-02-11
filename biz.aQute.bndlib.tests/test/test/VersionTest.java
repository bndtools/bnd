package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

public class VersionTest {

	@Test
	public void testSimple() {

		compare("[0,1)", "[0.5,0.8]", "[0.5.0,0.8.0]");
		compare("[0,1)", "[0.5,0.8)", "[0.5.0,0.8.0)");
		compare("[0,1)", "[0.5,2]", "[0.5.0,1.0.0)");
	}

	@Test
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

	void compare(String a, String b, String expected) {
		assertEquals(expected, new VersionRange(a).intersect(new VersionRange(b))
			.toString());
	}

	@Test
	public void testVersionRangeFilter() throws Exception {
		VersionRange range;
		Version version;
		Map<String, Object> map;
		Filter filter;

		range = VersionRange.parseOSGiVersionRange("1.2.3");
		filter = FrameworkUtil.createFilter(range.toFilter("version"));
		map = new HashMap<>();
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1.2.3");
		assertThat(range.includes(version)).isTrue();
		map.put("version", version);
		assertThat(filter.matches(map)).isTrue();

		version = new Version("2");
		assertThat(range.includes(version)).isTrue();
		map.put("version", version);
		assertThat(filter.matches(map)).isTrue();

		range = VersionRange.parseOSGiVersionRange("[1.2.3,2)");
		filter = FrameworkUtil.createFilter(range.toFilter("version"));
		map = new HashMap<>();
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1.2.3");
		assertThat(range.includes(version)).isTrue();
		map.put("version", version);
		assertThat(filter.matches(map)).isTrue();

		version = new Version("2");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();

		range = VersionRange.parseOSGiVersionRange("[1.2.3,2]");
		filter = FrameworkUtil.createFilter(range.toFilter("version"));
		map = new HashMap<>();
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1.2.3");
		assertThat(range.includes(version)).isTrue();
		map.put("version", version);
		assertThat(filter.matches(map)).isTrue();

		version = new Version("2");
		assertThat(range.includes(version)).isTrue();
		map.put("version", version);
		assertThat(filter.matches(map)).isTrue();

		range = VersionRange.parseOSGiVersionRange("(1.2.3,2)");
		filter = FrameworkUtil.createFilter(range.toFilter("version"));
		map = new HashMap<>();
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();

		version = new Version("1.2.3");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();

		version = new Version("2");
		assertThat(range.includes(version)).isFalse();
		map.put("version", version);
		assertThat(filter.matches(map)).isFalse();
	}
}
