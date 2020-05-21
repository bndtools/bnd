package aQute.lib.unmodifiable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

final class ImmutableMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
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
		return new ImmutableSet<>(entries, true);
	}

	@Override
	public boolean containsKey(Object key) {
		if (key != null) {
			for (Entry<K, V> entry : entries) {
				if (key.equals(entry.getKey())) {
					return true;
				}
			}
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
			for (Entry<K, V> entry : entries) {
				if (key.equals(entry.getKey())) {
					return entry.getValue();
				}
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
		int len = entries.length;
		Map<?, ?> other = (Map<?, ?>) o;
		if (len != other.size()) {
			return false;
		}
		try {
			for (int i = 0; i < len; i++) {
				Entry<K, V> entry = entries[i];
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
