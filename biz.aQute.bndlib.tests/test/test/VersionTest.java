package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
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

	@ParameterizedTest(name = "range={0}, version={1}, included={2}")
	@CsvSource(delimiter = '|', value = {
		"1.2.3|1|false", //
		"1.2.3|1.2.3|true", //
		"1.2.3|2|true", //
		"[1.2.3,2)|1|false", //
		"[1.2.3,2)|1.2.3|true", //
		"[1.2.3,2)|2|false", //
		"[1.2.3,2]|1|false", //
		"[1.2.3,2]|1.2.3|true", //
		"[1.2.3,2]|2|true", //
		"(1.2.3,2)|1|false", //
		"(1.2.3,2)|1.2.3|false", //
		"(1.2.3,2)|2|false" //
	})
	@DisplayName("VersionRange filter Testing")
	public void testVersionRangeFilter(@ConvertWith(VersionRangeConverter.class) VersionRange range,
		@ConvertWith(VersionConverter.class) Version version, boolean included) throws Exception {
		Filter filter = FrameworkUtil.createFilter(range.toFilter("version"));

		Map<String, Object> map = new HashMap<>();
		assertThat(filter.matches(map)).isFalse();

		assertThat(range.includes(version)).isEqualTo(included);
		map.put("version", version);
		assertThat(filter.matches(map)).isEqualTo(included);
	}

	static class VersionRangeConverter implements ArgumentConverter {
		@Override
		public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
			return VersionRange.parseOSGiVersionRange(source.toString());
		}
	}

	static class VersionConverter implements ArgumentConverter {
		@Override
		public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
			return Version.parseVersion(source.toString());
		}
	}
}
