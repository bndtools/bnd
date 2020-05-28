package aQute.lib.unmodifiable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

final class ImmutableMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
	@SuppressWarnings("unchecked")
	final static ImmutableMap<?, ?>	EMPTY	= new ImmutableMap<>();
	final Entry<K, V>[]				entries;
	final int[]						hash_bucket;
	transient Set<Entry<K, V>>		entrySet;

	@SafeVarargs
	ImmutableMap(Entry<K, V>... entries) {
		this.entries = entries;
		this.hash_bucket = hash(entries);
	}

	private static <K, V> int[] hash(Entry<K, V>[] entries) {
		int length = entries.length;
		if (length == 0) {
			return new int[1];
		}
		int[] hash_bucket = new int[length * 2];
		for (int i = 0; i < length;) {
			int slot = linear_probe(entries, hash_bucket, entries[i].getKey());
			if (slot >= 0) {
				throw new IllegalArgumentException("duplicate key: " + entries[i].getKey());
			}
			hash_bucket[-1 - slot] = ++i;
		}
		return hash_bucket;
	}

	// https://en.wikipedia.org/wiki/Linear_probing
	private static <K, V> int linear_probe(Entry<K, V>[] entries, int[] hash_bucket, Object key) {
		int length = hash_bucket.length;
		for (int hash = (key.hashCode() & 0x7FFF_FFFF) % length;; hash = (hash + 1) % length) {
			int slot = hash_bucket[hash] - 1;
			if (slot < 0) { // empty
				return -1 - hash;
			}
			if (entries[slot].getKey()
				.equals(key)) { // found
				return slot;
			}
		}
	}

	private int linear_probe(Object key) {
		return linear_probe(entries, hash_bucket, key);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> set = entrySet;
		if (set != null) {
			return set;
		}
		return entrySet = new ImmutableSet<>(entries);
	}

	@Override
	public int size() {
		return entries.length;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key != null) {
			return linear_probe(key) >= 0;
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value != null) {
			for (Entry<K, V> entry : entries) {
				if (value.equals(entry.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		if (key != null) {
			int slot = linear_probe(key);
			if (slot >= 0) {
				return entries[slot].getValue();
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Map)) {
			return false;
		}
		Map<?, ?> other = (Map<?, ?>) o;
		if (entries.length != other.size()) {
			return false;
		}
		try {
			for (Entry<K, V> entry : entries) {
				if (!entry.getValue()
					.equals(other.get(entry.getKey()))) {
					return false;
				}
			}
		} catch (ClassCastException checkedMap) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (Entry<K, V> entry : entries) {
			hashCode += entry.hashCode();
		}
		return hashCode;
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
}
