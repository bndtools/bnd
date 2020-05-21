package aQute.lib.unmodifiable;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class Maps {

	private Maps() {}

	public static <K, V> Map<K, V> of() {
		return new ImmutableMap<>();
	}

	public static <K, V> Map<K, V> of(K k1, V v1) {
		return new ImmutableMap<>(entry(k1, v1));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5),
			entry(k6, v6));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5),
			entry(k6, v6), entry(k7, v7));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7, K k8, V v8) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5),
			entry(k6, v6), entry(k7, v7), entry(k8, v8));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7, K k8, V v8, K k9, V v9) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5),
			entry(k6, v6), entry(k7, v7), entry(k8, v8), entry(k9, v9));
	}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5),
			entry(k6, v6), entry(k7, v7), entry(k8, v8), entry(k9, v9), entry(k10, v10));
	}

	@SafeVarargs
	public static <K, V> Map<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
		return new ImmutableMap<>(entries(Arrays.stream(entries)));
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> map) {
		if (map instanceof ImmutableMap) {
			return (Map<K, V>) map;
		}
		return new ImmutableMap<>(entries(map.entrySet()
			.stream()));
	}

	private static <K, V> Entry<K, V>[] entries(Stream<? extends Entry<? extends K, ? extends V>> stream) {
		return stream.map(e -> (e instanceof ImmutableEntry) ? e : entry(e.getKey(), e.getValue()))
			.toArray(Entry[]::new);
	}

	public static <K, V> Entry<K, V> entry(K key, V value) {
		return new ImmutableEntry<>(key, value);
	}
}
