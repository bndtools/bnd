package aQute.bnd.unmodifiable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;

import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;

public class MapsTest {

	@Test
	public void zero() {
		Map<String, String> map = Maps.of();
		assertThat(map).hasSize(0)
			.isEmpty();
		assertThat(map.containsKey("k3")).isFalse();
		assertThat(map.containsKey(null)).isFalse();
		assertThat(map.containsValue("v3")).isFalse();
		assertThat(map.containsValue(null)).isFalse();
		assertThat(map.get("k3")).isEqualTo(null);
		assertThat(map.get(null)).isEqualTo(null);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void one() {
		Map<String, String> map = Maps.of("k1", "v1");
		assertThat(map).hasSize(1)
			.containsEntry("k1", "v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void two() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		assertThat(map).hasSize(2)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void three() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3");
		assertThat(map).hasSize(3)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void four() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4");
		assertThat(map).hasSize(4)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void five() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5");
		assertThat(map).hasSize(5)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void six() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6");
		assertThat(map).hasSize(6)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void seven() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6", "k7",
			"v7");
		assertThat(map).hasSize(7)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void eight() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6", "k7",
			"v7", "k8", "v8");
		assertThat(map).hasSize(8)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7")
			.containsEntry("k8", "v8");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void nine() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6", "k7",
			"v7", "k8", "v8", "k9", "v9");
		assertThat(map).hasSize(9)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7")
			.containsEntry("k8", "v8")
			.containsEntry("k9", "v9");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void ten() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6", "k7",
			"v7", "k8", "v8", "k9", "v9", "k10", "v10");
		assertThat(map).hasSize(10)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2")
			.containsEntry("k3", "v3")
			.containsEntry("k4", "v4")
			.containsEntry("k5", "v5")
			.containsEntry("k6", "v6")
			.containsEntry("k7", "v7")
			.containsEntry("k8", "v8")
			.containsEntry("k9", "v9")
			.containsEntry("k10", "v10");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
	}

	@Test
	public void duplicate_key() {
		assertThatIllegalArgumentException().isThrownBy(() -> Maps.of("k1", "v1", "k1", "v2"));
		assertThatIllegalArgumentException().isThrownBy(() -> Maps.of("k1", "v1", "k2", "v2", "k2", "v3"));
		assertThatIllegalArgumentException().isThrownBy(() -> Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k3", "v4"));
	}

	@Test
	public void null_arguments() {
		assertThatNullPointerException().isThrownBy(() -> Maps.of("k1", "v1", null, "v2"));
		assertThatNullPointerException().isThrownBy(() -> Maps.of("k1", "v1", "k2", null));
		assertThatNullPointerException().isThrownBy(() -> Maps.entry(null, "v2"));
		assertThatNullPointerException().isThrownBy(() -> Maps.entry("k2", null));
		Map<String, String> nullKey = new HashMap<>();
		nullKey.put("k1", "v1");
		nullKey.put(null, "v2");
		assertThatNullPointerException().isThrownBy(() -> Maps.copyOf(nullKey));
		Map<String, String> nullValue = new HashMap<>();
		nullValue.put("k1", "v1");
		nullValue.put("k2", null);
		assertThatNullPointerException().isThrownBy(() -> Maps.copyOf(nullValue));
	}

	@Test
	public void entry() {
		Entry<String, String> entry = Maps.entry("k1", "v1");
		assertThat(entry.getKey()).isEqualTo("k1");
		assertThat(entry.getValue()).isEqualTo("v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entry.setValue("b"));
	}

	@Test
	public void entries() {
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = new Entry[2];
		entries[0] = new SimpleEntry<>("k1", "v1");
		entries[1] = new SimpleEntry<>("k2", "v2");
		Map<String, String> map = Maps.ofEntries(entries);
		entries[0].setValue("changed");
		entries[1] = new SimpleEntry<>("changed", "v2");
		assertThat(map).hasSize(2)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
		Entry<String, String> entry = map.entrySet()
			.iterator()
			.next();
		assertThat(entry.getKey()).isEqualTo("k1");
		assertThat(entry.getValue()).isEqualTo("v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entry.setValue("b"));
	}

	@Test
	public void copy() {
		Map<String, String> source = new LinkedHashMap<>();
		source.put("k1", "v1");
		source.put("k2", "v2");
		Map<String, String> map = Maps.copyOf(source);
		source.put("k2", "changed");
		assertThat(map).hasSize(2)
			.containsEntry("k1", "v1")
			.containsEntry("k2", "v2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.clear());
		Entry<String, String> entry = map.entrySet()
			.iterator()
			.next();
		assertThat(entry.getKey()).isEqualTo("k1");
		assertThat(entry.getValue()).isEqualTo("v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entry.setValue("b"));
	}

	@Test
	public void copy_unmodifiable() {
		Map<String, String> source = Maps.of("k1", "v1", "k2", "v2");
		Map<String, String> map = Maps.copyOf(source);
		assertThat(map).isSameAs(source);
	}

	@Test
	public void contains_key() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		assertThat(map.containsKey("k1")).isTrue();
		assertThat(map.containsKey("k2")).isTrue();
		assertThat(map.containsKey("k3")).isFalse();
		assertThat(map.containsKey(null)).isFalse();
	}

	@Test
	public void contains_value() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		assertThat(map.containsValue("v1")).isTrue();
		assertThat(map.containsValue("v2")).isTrue();
		assertThat(map.containsValue("v3")).isFalse();
		assertThat(map.containsValue(null)).isFalse();
	}

	@Test
	public void get() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		assertThat(map.get("k1")).isEqualTo("v1");
		assertThat(map.get("k2")).isEqualTo("v2");
		assertThat(map.get("k3")).isEqualTo(null);
		assertThat(map.get(null)).isEqualTo(null);
	}

	@Test
	public void hashcode() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put("k2", "v2");
		hashMap.put("k1", "v1");

		assertThat(map).hasSameHashCodeAs(hashMap);
		assertThat(map.entrySet()).hasSameHashCodeAs(hashMap.entrySet());
		assertThat(map.keySet()).hasSameHashCodeAs(hashMap.keySet());
	}

	@Test
	public void equals() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put("k2", "v2");
		hashMap.put("k1", "v1");

		assertThat(map).isEqualTo(hashMap);
		assertThat(map.entrySet()).isEqualTo(hashMap.entrySet());
		assertThat(map.keySet()).isEqualTo(hashMap.keySet());

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		hashMap.put("k2", "v1");
		assertThat(map).isNotEqualTo(hashMap);
		assertThat(map.entrySet()).isNotEqualTo(hashMap.entrySet());
		assertThat(map.keySet()).isEqualTo(hashMap.keySet());

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		hashMap.put("k3", "v3");
		assertThat(map).isNotEqualTo(hashMap);
		assertThat(map.entrySet()).isNotEqualTo(hashMap.entrySet());
		assertThat(map.keySet()).isNotEqualTo(hashMap.keySet());

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		assertThat(map).isNotEqualTo(hashMap);
		assertThat(map.entrySet()).isNotEqualTo(hashMap.entrySet());
		assertThat(map.keySet()).isNotEqualTo(hashMap.keySet());

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		hashMap.put("k2", "v2");
		hashMap.put("k3", "v3");
		assertThat(map).isNotEqualTo(hashMap);
		assertThat(map.entrySet()).isNotEqualTo(hashMap.entrySet());
		assertThat(map.keySet()).isNotEqualTo(hashMap.keySet());
	}

	@Test
	public void entry_set_contains() {
		Set<Entry<String, String>> entrySet = Maps.of("k1", "v1", "k2", "v2")
			.entrySet();
		assertThat(entrySet).containsExactly(new SimpleEntry<>("k1", "v1"), new SimpleEntry<>("k2", "v2"));

		assertThat(entrySet.contains(new SimpleEntry<>("k1", "v1"))).isTrue();
		assertThat(entrySet.contains(new SimpleEntry<>("k2", "v2"))).isTrue();
		assertThat(entrySet.contains(new SimpleEntry<>("k1", "v2"))).isFalse();
		assertThat(entrySet.contains(null)).isFalse();

		entrySet = Maps.<String, String> of()
			.entrySet();
		assertThat(entrySet.contains(new SimpleEntry<>("k1", "v2"))).isFalse();
		assertThat(entrySet.contains(null)).isFalse();
	}

	@Test
	public void entry_set_stream() {
		Set<Entry<String, String>> entrySet = Maps.of("k1", "v1", "k2", "v2")
			.entrySet();
		assertThat(entrySet.stream()).containsExactly(new SimpleEntry<>("k1", "v1"), new SimpleEntry<>("k2", "v2"));
	}

	@Test
	public void entry_set_foreach() {
		Set<Entry<String, String>> entrySet = Maps.of("k1", "v1", "k2", "v2")
			.entrySet();
		Set<Entry<String, String>> set = new HashSet<>();
		entrySet.forEach(set::add);
		assertThat(set).containsExactlyInAnyOrder(new SimpleEntry<>("k1", "v1"), new SimpleEntry<>("k2", "v2"));
	}

	@Test
	public void entry_set_iterator() {
		Set<Entry<String, String>> entrySet = Maps.of("k1", "v1", "k2", "v2")
			.entrySet();
		Iterator<Entry<String, String>> iterator = entrySet.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(new SimpleEntry<>("k1", "v1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(new SimpleEntry<>("k2", "v2"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> iterator.next());
	}

	@Test
	public void entry_set_iterator_empty() {
		Set<Entry<String, String>> entrySet = Maps.<String, String> of()
			.entrySet();
		Iterator<Entry<String, String>> iterator = entrySet.iterator();
		assertThat(iterator.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> iterator.next());
	}

	static class Holder<E> implements Consumer<E> {
		E value;

		@Override
		public void accept(E t) {
			this.value = t;
		}
	}

	@Test
	public void entry_set_spliterator() {
		final int max = (1 << Short.SIZE) - 1;
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = new Entry[max];
		for (int i = 0; i < max; i++) {
			entries[i] = Maps.entry(String.format("k%d", i + 1), String.format("v%d", i + 1));
		}
		Map<String, String> map = Maps.ofEntries(entries);
		assertThat(map).hasSize(max);
		Set<Entry<String, String>> entrySet = map.entrySet();
		Spliterator<Entry<String, String>> spliterator = entrySet.spliterator();
		assertThat(spliterator).hasCharacteristics(Spliterator.DISTINCT);
		assertThat(spliterator.estimateSize()).isEqualTo(entrySet.size());
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(entrySet.size());

		Spliterator<Entry<String, String>> trySplit = spliterator.trySplit();
		assertThat(trySplit).hasCharacteristics(Spliterator.DISTINCT);
		assertThat(trySplit.getExactSizeIfKnown() + spliterator.getExactSizeIfKnown()).isEqualTo(entrySet.size());

		long firstSize = trySplit.getExactSizeIfKnown();
		Holder<Entry<String, String>> holder = new Holder<>();
		assertThat(trySplit.tryAdvance(holder)).isTrue();
		assertThat(holder.value).isEqualTo(new SimpleEntry<>("k1", "v1"));
		trySplit.forEachRemaining(holder);
		assertThat(trySplit.tryAdvance(holder)).isFalse();
		assertThat(holder.value)
			.isEqualTo(new SimpleEntry<>(String.format("k%d", firstSize), String.format("v%d", firstSize)));
		assertThat(trySplit.trySplit()).isNull();

		long splitPoint = firstSize + 1;
		assertThat(spliterator.tryAdvance(holder)).isTrue();
		assertThat(holder.value)
			.isEqualTo(new SimpleEntry<>(String.format("k%d", splitPoint), String.format("v%d", splitPoint)));
		while (spliterator.tryAdvance(holder)) {}
		assertThat(holder.value).isEqualTo(new SimpleEntry<>(String.format("k%d", max), String.format("v%d", max)));
		assertThat(spliterator.trySplit()).isNull();
	}

	@Test
	public void entry_set_spliterator_empty() {
		Set<Entry<String, String>> entrySet = Maps.<String, String> of()
			.entrySet();
		Spliterator<Entry<String, String>> spliterator = entrySet.spliterator();
		assertThat(spliterator).hasCharacteristics(Spliterator.DISTINCT);
		assertThat(spliterator.estimateSize()).isEqualTo(entrySet.size());
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(entrySet.size());

		assertThat(spliterator.trySplit()).isNull();

		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(entrySet.size());
		Holder<Entry<String, String>> holder = new Holder<>();
		assertThat(spliterator.tryAdvance(holder)).isFalse();
	}

	@Test
	public void key_set_contains() {
		Set<String> keySet = Maps.of("k1", "v1", "k2", "v2")
			.keySet();
		assertThat(keySet).containsExactly("k1", "k2");

		assertThat(keySet.contains("k1")).isTrue();
		assertThat(keySet.contains("k2")).isTrue();
		assertThat(keySet.contains("k3")).isFalse();
		assertThat(keySet.contains(null)).isFalse();

		keySet = Maps.<String, String> of()
			.keySet();
		assertThat(keySet.contains("k1")).isFalse();
		assertThat(keySet.contains(null)).isFalse();
	}

	@Test
	public void key_set_stream() {
		Set<String> keySet = Maps.of("k1", "v1", "k2", "v2")
			.keySet();
		assertThat(keySet.stream()).containsExactly("k1", "k2");
	}

	@Test
	public void key_set_foreach() {
		Set<String> keySet = Maps.of("k1", "v1", "k2", "v2")
			.keySet();
		Set<String> set = new HashSet<>();
		keySet.forEach(set::add);
		assertThat(set).containsExactlyInAnyOrder("k1", "k2");
	}

	@Test
	public void key_set_iterator() {
		Set<String> keySet = Maps.of("k1", "v1", "k2", "v2")
			.keySet();
		Iterator<String> iterator = keySet.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("k1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("k2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> iterator.next());
	}

	@Test
	public void key_set_iterator_empty() {
		Set<String> keySet = Maps.<String, String> of()
			.keySet();
		Iterator<String> iterator = keySet.iterator();
		assertThat(iterator.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> iterator.next());
	}

	@Test
	public void key_set_spliterator() {
		final int max = (1 << Short.SIZE) - 1;
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = new Entry[max];
		for (int i = 0; i < max; i++) {
			entries[i] = Maps.entry(String.format("k%d", i + 1), String.format("v%d", i + 1));
		}
		Map<String, String> map = Maps.ofEntries(entries);
		assertThat(map).hasSize(max);
		Set<String> keySet = map.keySet();
		Spliterator<String> spliterator = keySet.spliterator();
		assertThat(spliterator).hasCharacteristics(Spliterator.DISTINCT);
		assertThat(spliterator.estimateSize()).isEqualTo(keySet.size());
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(keySet.size());

		Spliterator<String> trySplit = spliterator.trySplit();
		assertThat(trySplit).hasCharacteristics(Spliterator.DISTINCT);
		assertThat(trySplit.getExactSizeIfKnown() + spliterator.getExactSizeIfKnown()).isEqualTo(keySet.size());

		long firstSize = trySplit.getExactSizeIfKnown();
		Holder<String> holder = new Holder<>();
		assertThat(trySplit.tryAdvance(holder)).isTrue();
		assertThat(holder.value).isEqualTo("k1");
		trySplit.forEachRemaining(holder);
		assertThat(trySplit.tryAdvance(holder)).isFalse();
		assertThat(holder.value).isEqualTo(String.format("k%d", firstSize));
		assertThat(trySplit.trySplit()).isNull();

		long splitPoint = firstSize + 1;
		assertThat(spliterator.tryAdvance(holder)).isTrue();
		assertThat(holder.value).isEqualTo(String.format("k%d", splitPoint));
		while (spliterator.tryAdvance(holder)) {}
		assertThat(holder.value).isEqualTo(String.format("k%d", max));
		assertThat(spliterator.trySplit()).isNull();
	}

	@Test
	public void key_set_spliterator_empty() {
		Set<String> keySet = Maps.<String, String> of()
			.keySet();
		Spliterator<String> spliterator = keySet.spliterator();
		assertThat(spliterator).hasCharacteristics(Spliterator.DISTINCT);
		assertThat(spliterator.estimateSize()).isEqualTo(keySet.size());
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(keySet.size());

		assertThat(spliterator.trySplit()).isNull();

		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(keySet.size());
		Holder<String> holder = new Holder<>();
		assertThat(spliterator.tryAdvance(holder)).isFalse();
	}

	@Test
	public void value_collection_contains() {
		Collection<String> values = Maps.of("k1", "v1", "k2", "v2", "k3", "v1")
			.values();
		assertThat(values).containsExactly("v1", "v2", "v1");

		assertThat(values.contains("v1")).isTrue();
		assertThat(values.contains("v2")).isTrue();
		assertThat(values.contains("v3")).isFalse();
		assertThat(values.contains(null)).isFalse();

		values = Maps.<String, String> of()
			.values();
		assertThat(values.contains("v1")).isFalse();
		assertThat(values.contains(null)).isFalse();
	}

	@Test
	public void value_collection_stream() {
		Collection<String> values = Maps.of("k1", "v1", "k2", "v2", "k3", "v1")
			.values();
		assertThat(values.stream()).containsExactly("v1", "v2", "v1");
		assertThat(values.stream()
			.distinct()).containsExactly("v1", "v2");
	}

	@Test
	public void value_collection_foreach() {
		Collection<String> values = Maps.of("k1", "v1", "k2", "v2", "k3", "v1")
			.values();
		List<String> list = new ArrayList<>();
		values.forEach(list::add);
		assertThat(list).containsExactly("v1", "v2", "v1");
	}

	@Test
	public void value_collection_iterator() {
		Collection<String> values = Maps.of("k1", "v1", "k2", "v2", "k3", "v1")
			.values();
		Iterator<String> iterator = values.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("v2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("v1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.remove());
		assertThat(iterator.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> iterator.next());
	}

	@Test
	public void value_collection_iterator_empty() {
		Collection<String> values = Maps.<String, String> of()
			.values();
		Iterator<String> iterator = values.iterator();
		assertThat(iterator.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> iterator.next());
	}

	@Test
	public void value_collection_spliterator() {
		final int max = (1 << Short.SIZE) - 1;
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = new Entry[max];
		for (int i = 0; i < max; i++) {
			entries[i] = Maps.entry(String.format("k%d", i + 1), String.format("v%d", i + 1));
		}
		Map<String, String> map = Maps.ofEntries(entries);
		assertThat(map).hasSize(max);
		Collection<String> values = map.values();
		Spliterator<String> spliterator = values.spliterator();
		assertThat(spliterator.characteristics() & Spliterator.DISTINCT).isZero();
		assertThat(spliterator.estimateSize()).isEqualTo(values.size());
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(values.size());

		Spliterator<String> trySplit = spliterator.trySplit();
		assertThat(trySplit.characteristics() & Spliterator.DISTINCT).isZero();
		assertThat(trySplit.getExactSizeIfKnown() + spliterator.getExactSizeIfKnown()).isEqualTo(values.size());

		long firstSize = trySplit.getExactSizeIfKnown();
		Holder<String> holder = new Holder<>();
		assertThat(trySplit.tryAdvance(holder)).isTrue();
		assertThat(holder.value).isEqualTo("v1");
		trySplit.forEachRemaining(holder);
		assertThat(trySplit.tryAdvance(holder)).isFalse();
		assertThat(holder.value).isEqualTo(String.format("v%d", firstSize));
		assertThat(trySplit.trySplit()).isNull();

		long splitPoint = firstSize + 1;
		assertThat(spliterator.tryAdvance(holder)).isTrue();
		assertThat(holder.value).isEqualTo(String.format("v%d", splitPoint));
		while (spliterator.tryAdvance(holder)) {}
		assertThat(holder.value).isEqualTo(String.format("v%d", max));
		assertThat(spliterator.trySplit()).isNull();
	}

	@Test
	public void value_collection_spliterator_empty() {
		Collection<String> values = Maps.<String, String> of()
			.values();
		Spliterator<String> spliterator = values.spliterator();
		assertThat(spliterator.characteristics() & Spliterator.DISTINCT).isZero();
		assertThat(spliterator.estimateSize()).isEqualTo(values.size());
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(values.size());

		assertThat(spliterator.trySplit()).isNull();

		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(values.size());
		Holder<String> holder = new Holder<>();
		assertThat(spliterator.tryAdvance(holder)).isFalse();
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

		Map<String, String> map = Maps.of("polygenelubricants", "v1", "GydZG_", "v2", "DESIGNING WORKHOUSES", "v3");

		assertThat(map).hasSize(3)
			.containsEntry("polygenelubricants", "v1")
			.containsEntry("GydZG_", "v2")
			.containsEntry("DESIGNING WORKHOUSES", "v3");
	}

	@Test
	public void max_entries() {
		final int max = (1 << Short.SIZE) - 1;
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = new Entry[max];
		for (int i = 0; i < max; i++) {
			entries[i] = Maps.entry(String.format("k%d", i + 1), String.format("v%d", i + 1));
		}
		Map<String, String> map = Maps.ofEntries(entries);
		MapAssert<String, String> assertion = assertThat(map).hasSize(max);
		for (int i = 0; i < max; i++) {
			assertion.containsEntry(entries[i].getKey(), entries[i].getValue());
		}
	}

	@Test
	public void over_max_entries() {
		final int over_max = (1 << Short.SIZE);
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = new Entry[over_max];
		for (int i = 0; i < over_max; i++) {
			entries[i] = Maps.entry(String.format("k%d", i + 1), String.format("v%d", i + 1));
		}
		assertThatIllegalArgumentException().isThrownBy(() -> Maps.ofEntries(entries));
	}

	@Test
	public void serialization() throws Exception {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5");
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(map);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(bos.toByteBuffer()));
		@SuppressWarnings("unchecked")
		Map<String, String> deser = (Map<String, String>) ois.readObject();

		assertThat(deser).isEqualTo(map)
			.isNotSameAs(map)
			.containsExactlyEntriesOf(map);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("k1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.clear());
	}

	@Test
	public void serialization_zero() throws Exception {
		Map<String, String> map = Maps.of();
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(map);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(bos.toByteBuffer()));
		@SuppressWarnings("unchecked")
		Map<String, String> deser = (Map<String, String>) ois.readObject();

		assertThat(deser).isSameAs(map);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.put("a", "b"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.clear());
	}

}
