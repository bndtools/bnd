package aQute.lib.unmodifiable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7");
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
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7", "k8", "v8");
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
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7", "k8", "v8", "k9", "v9");
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
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6",
			"k7", "v7", "k8", "v8", "k9", "v9", "k10", "v10");
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
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Maps.of("k1", "v1", "k2", "v2", "k3", "v3", "k3", "v4"));
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
		entries[0] = new SimpleEntry<String, String>("k1", "v1");
		entries[1] = new SimpleEntry<String, String>("k2", "v2");
		Map<String, String> map = Maps.ofEntries(entries);
		entries[0].setValue("changed");
		entries[1] = new SimpleEntry<String, String>("changed", "v2");
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

		assertThat(map.hashCode()).isEqualTo(hashMap.hashCode());
	}

	@Test
	public void equals() {
		Map<String, String> map = Maps.of("k1", "v1", "k2", "v2");
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put("k2", "v2");
		hashMap.put("k1", "v1");

		assertThat(map).isEqualTo(hashMap);

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		hashMap.put("k2", "v1");
		assertThat(map).isNotEqualTo(hashMap);

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		hashMap.put("k3", "v3");
		assertThat(map).isNotEqualTo(hashMap);

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		assertThat(map).isNotEqualTo(hashMap);

		hashMap = new HashMap<>();
		hashMap.put("k1", "v1");
		hashMap.put("k2", "v2");
		hashMap.put("k3", "v3");
		assertThat(map).isNotEqualTo(hashMap);
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

}
