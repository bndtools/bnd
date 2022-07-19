package aQute.bnd.osgi.resource;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

class DeferredValueMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
	private final Map<K, V> map;

	DeferredValueMap(Map<K, V> map) {
		this.map = map;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		V v = map.get(key);
		if (v instanceof DeferredValue) {
			v = ((DeferredValue<V>) v).get();
		}
		return v;
	}

	/**
	 * This get method will not unwrap a DeferredValue.
	 *
	 * @param key The map key.
	 * @return The DeferredValue or value.
	 */
	Object getDeferred(Object key) {
		Object v = map.get(key);
		return v;
	}

	@Override
	public boolean equals(Object o) {
		return map.equals(o);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet<>(map);
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	final static class EntrySet<K, V> extends AbstractSet<Entry<K, V>> {
		private final Set<Entry<K, V>> entries;

		EntrySet(Map<K, V> map) {
			this.entries = map.entrySet();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIterator<>(entries);
		}

		@Override
		public int size() {
			return entries.size();
		}
	}

	final static class EntryIterator<K, V> implements Iterator<Entry<K, V>> {
		private final Iterator<Entry<K, V>> iterator;

		EntryIterator(Set<Entry<K, V>> entries) {
			this.iterator = entries.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Entry<K, V> next() {
			Entry<K, V> entry = iterator.next();
			V v = entry.getValue();
			if (v instanceof DeferredValue) {
				@SuppressWarnings("unchecked")
				DeferredValue<V> deferred = (DeferredValue<V>) v;
				return new DeferredValueEntry<>(entry.getKey(), deferred);
			}
			return entry;
		}
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
