package aQute.bnd.unmodifiable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;

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
		String[] entries = new String[] {
			"e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10", "e11"
		};
		Set<String> set = Sets.of(entries);
		entries[0] = "changed";
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
		source.set(0, "changed");
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

		set = Sets.of();
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

	// Strings can have a hashCode of Integer.MIN_VALUE. For example:
	// "polygenelubricants", "GydZG_", "DESIGNING WORKHOUSES"
	@Test
	public void hash_min_value() {
		assertThat("polygenelubricants".hashCode()).as("polygenelubricants")
			.isEqualTo(Integer.MIN_VALUE);
		assertThat("GydZG_".hashCode()).as("GydZG_")
			.isEqualTo(Integer.MIN_VALUE);
		assertThat("DESIGNING WORKHOUSES".hashCode()).as("DESIGNING WORKHOUSES")
			.isEqualTo(Integer.MIN_VALUE);

		Set<String> set = Sets.of("e1", "polygenelubricants", "GydZG_", "DESIGNING WORKHOUSES", "e5");

		assertThat(set).containsExactlyInAnyOrder("e5", "polygenelubricants", "GydZG_", "DESIGNING WORKHOUSES", "e1");
	}

	@Test
	public void max_entries() {
		final int max = (1 << Short.SIZE) - 1;
		String[] entries = new String[max];
		for (int i = 0; i < max; i++) {
			entries[i] = String.format("e%d", i + 1);
		}
		Set<String> set = Sets.of(entries);
		assertThat(set).hasSize(max);
		for (int i = 0; i < max; i++) {
			assertThat(set.contains(entries[i])).as("contains(%s)", entries[i])
				.isTrue();
		}
	}

	@Test
	public void over_max_entries() {
		final int over_max = (1 << Short.SIZE);
		String[] entries = new String[over_max];
		for (int i = 0; i < over_max; i++) {
			entries[i] = String.format("e%d", i + 1);
		}
		assertThatIllegalArgumentException().isThrownBy(() -> Sets.of(entries));
	}

	@Test
	public void serialization() throws Exception {
		Set<String> set = Sets.of("e1", "e2", "e3", "e4", "e5");
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(set);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(bos.toByteBuffer()));
		@SuppressWarnings("unchecked")
		Set<String> deser = (Set<String>) ois.readObject();

		assertThat(deser).isEqualTo(set)
			.isNotSameAs(set)
			.containsExactlyElementsOf(set);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.clear());
	}

	@Test
	public void serialization_zero() throws Exception {
		Set<String> set = Sets.of();
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(set);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(bos.toByteBuffer()));
		@SuppressWarnings("unchecked")
		Set<String> deser = (Set<String>) ois.readObject();

		assertThat(deser).isSameAs(set);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.clear());
	}

}
