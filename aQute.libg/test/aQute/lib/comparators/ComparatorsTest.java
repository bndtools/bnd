package aQute.lib.comparators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ComparatorsTest {

	@Test
	void testCompareNull() {
		assertThat(Comparators.compareNull(null, null)).isEqualTo(0);
		assertThat(Comparators.compareNull("A", null)).isEqualTo(1);
		assertThat(Comparators.compareNull(null, "A")).isEqualTo(-1);
		assertThat(Comparators.compareNull("A", "A")).isEqualTo(Integer.MAX_VALUE);
		assertThat(Comparators.compareNull("A", "B")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void testCompareNullEquals() {
		assertThat(Comparators.compareNullEquals(null, null)).isEqualTo(0);
		assertThat(Comparators.compareNullEquals("A", null)).isEqualTo(1);
		assertThat(Comparators.compareNullEquals(null, "A")).isEqualTo(-1);
		assertThat(Comparators.compareNullEquals("A", "A")).isEqualTo(0);
		assertThat(Comparators.compareNullEquals("A", "B")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void testIfDecided() {
		assertThat(Comparators.isDecided(-1)).isTrue();
		assertThat(Comparators.isDecided(0)).isTrue();
		assertThat(Comparators.isDecided(1)).isTrue();
		assertThat(Comparators.isDecided(Integer.MAX_VALUE)).isFalse();
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

	}
}
