package aQute.lib.unmodifiable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class SetsTest {

	@Test
	public void zero() {
		Set<String> set = Sets.of();
		assertThat(set).hasSize(0)
			.isEmpty();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void one() {
		Set<String> set = Sets.of("e1");
		assertThat(set).hasSize(1)
			.containsExactlyInAnyOrder("e1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void two() {
		Set<String> set = Sets.of("e1", "e2");
		assertThat(set).hasSize(2)
			.containsExactlyInAnyOrder("e1", "e2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void three() {
		Set<String> set = Sets.of("e1", "e2", "e3");
		assertThat(set).hasSize(3)
			.containsExactlyInAnyOrder("e1", "e2", "e3");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void four() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4");
		assertThat(set).hasSize(4)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void five() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5");
		assertThat(set).hasSize(5)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void six() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5", "e6");
		assertThat(set).hasSize(6)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", "e6");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void seven() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5", "e6", "e7");
		assertThat(set).hasSize(7)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", "e6", "e7");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void eight() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8");
		assertThat(set).hasSize(8)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void nine() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9");
		assertThat(set).hasSize(9)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void ten() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10");
		assertThat(set).hasSize(10)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void entries() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10", "e11");
		assertThat(set).hasSize(11)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10", "e11");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void duplicate_element() {
		assertThatIllegalArgumentException().isThrownBy(() -> Sets.of("e1", "e1"));
		assertThatIllegalArgumentException().isThrownBy(() -> Sets.of("e1", "e2", "e2"));
		assertThatIllegalArgumentException().isThrownBy(() -> Sets.of("e1", "e2", "e3", "e3"));
	}

	@Test
	public void null_arguments() {
		assertThatNullPointerException().isThrownBy(() -> Sets.of("e1", null));
		assertThatNullPointerException().isThrownBy(() -> Sets.of("e1", "e2", null));
		Set<String> nullElement = new HashSet<>();
		nullElement.add("e1");
		nullElement.add(null);
		assertThatNullPointerException().isThrownBy(() -> Sets.copyOf(nullElement));
	}

	@Test
	public void copy() {
		List<String> source = new ArrayList<>();
		source.add("e1");
		source.add("e2");
		source.add("e1");
		Set<String> set = Sets.copyOf(source);
		assertThat(set).hasSize(2)
			.containsExactlyInAnyOrder("e1", "e2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.clear());
	}

	@Test
	public void copy_unmodifiable() {
		Set<String> source = Sets.of("e1", "e2");
		Set<String> set = Sets.copyOf(source);
		assertThat(set).isSameAs(source);
	}

	@Test
	public void array() {
		Set<String> source = Sets.of("e1", "e2", "e3", "e4", "e5");
		Object[] array = source.toArray();
		assertThat(array).hasSize(5)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5");
	}

	@Test
	public void array_string() {
		Set<String> source = Sets.of("e1", "e2", "e3", "e4", "e5");
		String[] target = new String[0];
		String[] array = source.toArray(target);
		assertThat(array).isNotSameAs(target)
			.hasSize(5)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5");

		target = new String[source.size() + 1];
		array = source.toArray(target);
		assertThat(array).isSameAs(target)
			.containsExactlyInAnyOrder("e1", "e2", "e3", "e4", "e5", null);
		assertThat(array[target.length - 1]).isNull();
	}

	@Test
	public void contains() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5");
		assertThat(set.contains("e1")).isTrue();
		assertThat(set.contains("e3")).isTrue();
		assertThat(set.contains("e5")).isTrue();
		assertThat(set.contains("e6")).isFalse();
		assertThat(set.contains(null)).isFalse();
	}

	@Test
	public void hashcode() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5");
		Set<String> hashSet = new HashSet<>(Arrays.asList("e5", "e2", "e3", "e4", "e1"));

		assertThat(set.hashCode()).isEqualTo(hashSet.hashCode());
	}

	@Test
	public void equals() {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5");
		Set<String> hashSet = new HashSet<>(Arrays.asList("e5", "e2", "e3", "e4", "e1"));

		assertThat(set).isEqualTo(hashSet);
		assertThat(set).isNotEqualTo(new HashSet<>(Arrays.asList("e1", "e2", "e3", "e4")));
		assertThat(set).isNotEqualTo(new HashSet<>(Arrays.asList("e1", "e2", "e3", "e4", "e6")));
		assertThat(set).isNotEqualTo(new HashSet<>(Arrays.asList("e1", "e2", "e3", "e4", "e5", "e6")));
	}

}
