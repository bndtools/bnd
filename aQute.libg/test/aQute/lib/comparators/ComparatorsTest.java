package aQute.lib.comparators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ComparatorsTest {

	@Test
	void testCompareNull() {
		assertThat(Comparators.comparePresent(null, null)).isEqualTo(0);
		assertThat(Comparators.comparePresent("A", null)).isEqualTo(1);
		assertThat(Comparators.comparePresent(null, "A")).isEqualTo(-1);
		assertThat(Comparators.comparePresent("A", "A")).isEqualTo(Integer.MAX_VALUE);
		assertThat(Comparators.comparePresent("A", "B")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void testCompareNullComparator() {
		assertThat(Comparators.compare(null, null, () -> 100)).isEqualTo(0);
		assertThat(Comparators.compare("A", null, () -> 100)).isEqualTo(1);
		assertThat(Comparators.compare(null, "A", () -> 100)).isEqualTo(-1);
		assertThat(Comparators.compare("A", "A", (a, b) -> a.compareTo(b))).isEqualTo(0);
		assertThat(Comparators.compare("A", "B", (a, b) -> a.compareTo(b))).isEqualTo(-1);
	}

	@Test
	void testCompareNullEquals() {
		assertThat(Comparators.comparePresentEquals(null, null)).isEqualTo(0);
		assertThat(Comparators.comparePresentEquals("A", null)).isEqualTo(1);
		assertThat(Comparators.comparePresentEquals(null, "A")).isEqualTo(-1);
		assertThat(Comparators.comparePresentEquals("A", "A")).isEqualTo(0);
		assertThat(Comparators.comparePresentEquals("A", "B")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void testIfDecided() {
		assertThat(Comparators.isFinal(10)).isTrue();
		assertThat(Comparators.isFinal(-1)).isTrue();
		assertThat(Comparators.isFinal(0)).isTrue();
		assertThat(Comparators.isFinal(1)).isTrue();
		assertThat(Comparators.isFinal(Integer.MAX_VALUE)).isFalse();
	}

	@Test
	void tesCompare() {
		assertThat(Comparators.compare(null, null)).isEqualTo(0);
		assertThat(Comparators.compare("A", null)).isEqualTo(1);
		assertThat(Comparators.compare(null, "A")).isEqualTo(-1);
		assertThat(Comparators.compare("A", "A")).isEqualTo(0);
		assertThat(Comparators.compare("A", "B")).isEqualTo(-1);

		assertThat(Comparators.compare(new String[] {
			"A"
		}, new String[] {
			"A", "B"
		})).isEqualTo(-1);
		assertThat(Comparators.compare(new String[] {
			"A", "B"
		}, new String[] {
			"A"
		})).isEqualTo(1);
		assertThat(Comparators.compare(new String[] {
			"A", "B"
		}, new String[] {
			"A", "B"
		})).isEqualTo(0);
		assertThat(Comparators.compare(new String[] {
			"A", "A"
		}, new String[] {
			"A", "B"
		})).isEqualTo(-1);

		List<String> la = Arrays.asList("A");
		List<String> lb = Arrays.asList("B");
		List<String> lab1 = Arrays.asList("A", "B");
		List<String> lab2 = Arrays.asList("A", "B");
		assertThat(Comparators.compare(la, lb)).isEqualTo(-1);
		assertThat(Comparators.compare(la, lab1)).isEqualTo(-1);
		assertThat(Comparators.compare(lb, la)).isEqualTo(1);
		assertThat(Comparators.compare(lab1, la)).isEqualTo(1);
		assertThat(Comparators.compare(la, lb)).isEqualTo(-1);
		assertThat(Comparators.compare(lab1, lab2)).isEqualTo(0);

		Object a = new Object();
		Object b = new Object();

		assertThat(Comparators.compare(a, a)).isEqualTo(0);
		assertThat(Comparators.compare(a, b)).isEqualTo(0);
		assertThat(Comparators.compare(b, a)).isEqualTo(0);

		assertThat(Comparators.compare(b.getClass()
			.getName(),
			"b".getClass()
				.getName())).isEqualTo(-1);
		assertThat(Comparators.compare(b, "b")).isEqualTo(-1);

	}

	@Test
	void testCompareRecurse() {

		List<Object> l1 = new ArrayList<>();
		l1.add(l1);

		List<Object> l2 = new ArrayList<>();
		l2.add(l1);

		assertThat(Comparators.compare(l1, l2, 10)).isEqualTo(0);

		assertThat(Comparators.compare("A", "B", 0)).isEqualTo(0);
		assertThat(Comparators.compare("A", "B", 1)).isEqualTo(-1);
	}

	@Test
	void testMaps() {

		Map<String, Object> a = map("a", 1, "b", 2);
		Map<String, Object> aa = map("a", 1, "b", 2);
		Map<String, Object> b = map("a", 1, "b", 3);
		Map<String, Object> c = map("a", 1, "c", 1);

		assertThat(Comparators.compareMapsByKeys(a, null, "a", "b")).isEqualTo(1);
		assertThat(Comparators.compareMapsByKeys(null, null, "a", "b")).isEqualTo(0);
		assertThat(Comparators.compareMapsByKeys(null, a, "a", "b")).isEqualTo(-1);

		assertThat(Comparators.compareMapsByKeys(a, b, "a", "b")).isEqualTo(-1);
		assertThat(Comparators.compareMapsByKeys(b, a, "a", "b")).isEqualTo(1);
		assertThat(Comparators.compareMapsByKeys(b, a, "a")).isEqualTo(0);
		assertThat(Comparators.compareMapsByKeys(a, b, "b")).isEqualTo(-1);
		assertThat(Comparators.compareMapsByKeys(a, aa, "a", "b")).isEqualTo(0);
		assertThat(Comparators.compareMapsByKeys(a, aa)).isEqualTo(0);

		assertThat(Comparators.compareMapsByKeys(a, b, "a", "b")).isEqualTo(-1);

		assertThat(Comparators.compareMapsByKeys(a, c, "a", "b")).isEqualTo(1);
		assertThat(Comparators.compareMapsByKeys(a, c, "a", "c")).isEqualTo(-1);
		assertThat(Comparators.compareMapsByKeys(a, c, "a")).isEqualTo(0);
		assertThat(Comparators.compareMapsByKeys(a, c, "x")).isEqualTo(0);
	}

	private Map<String, Object> map(Object... keys) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < keys.length; i += 2) {
			String key = (String) keys[i];
			map.put(key, keys[i + 1]);
		}
		return map;
	}
}
