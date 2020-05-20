package aQute.lib.map;

import static java.util.Objects.requireNonNull;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class Maps {

	private Maps() {}

	public static <K, V> Map<K, V> mapOf() {
		return new ImmutableMap<>();
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1) {
		return new ImmutableMap<>(entry(k1, v1));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5), entry(k6, v6));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5), entry(k6, v6),
			entry(k7, v7));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7, K k8, V v8) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5), entry(k6, v6),
			entry(k7, v7), entry(k8, v8));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7, K k8, V v8, K k9, V v9) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5), entry(k6, v6),
			entry(k7, v7), entry(k8, v8), entry(k9, v9));
	}

	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7,
		V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
		return new ImmutableMap<>(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4), entry(k5, v5), entry(k6, v6),
			entry(k7, v7), entry(k8, v8), entry(k9, v9), entry(k10, v10));
	}

	static <K, V> Entry<K, V> entry(K key, V value) {
		return new SimpleImmutableEntry<>(requireNonNull(key), requireNonNull(value));
	}

	static final class ImmutableMap<K, V> extends AbstractMap<K, V> {
		final Entry<K, V>[] entries;

		@SafeVarargs
		ImmutableMap(Entry<K, V>... entries) {
			this.entries = entries;
			for (int i = 0, len = entries.length, outer = len - 1; i < outer; i++) {
				K key = entries[i].getKey();
				for (int j = i + 1; j < len; j++) {
					if (key.equals(entries[j].getKey())) {
						throw new IllegalArgumentException("duplicate key: " + key);
					}
				}
			}
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return new EntrySet();
		}

		@Override
		public V put(K key, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> map) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V putIfAbsent(K key, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean replace(K key, V oldValue, V newValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V replace(K key, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
			throw new UnsupportedOperationException();
		}

		final class EntrySet extends AbstractSet<Entry<K, V>> {
			EntrySet() {}

			@Override
			public Iterator<Entry<K, V>> iterator() {
				return new EntryIterator();
			}

			@Override
			public int size() {
				return entries.length;
			}

			@Override
			public boolean add(Entry<K, V> e) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean remove(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean addAll(Collection<? extends Entry<K, V>> collection) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean removeAll(Collection<?> collection) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean retainAll(Collection<?> collection) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean removeIf(Predicate<? super Entry<K, V>> filter) {
				throw new UnsupportedOperationException();
			}
		}

		final class EntryIterator implements Iterator<Entry<K, V>> {
			private int index = 0;

			EntryIterator() {}

			@Override
			public boolean hasNext() {
				return index < entries.length;
			}

			@Override
			public Entry<K, V> next() {
				if (hasNext()) {
					return entries[index++];
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
	}
}
