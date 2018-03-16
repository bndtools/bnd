package aQute.lib.collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DoubleKeyMap<K1, K2, V> extends HashMap<K1, Map<K2, V>> implements Map<K1, Map<K2, V>> {
	private static final long	serialVersionUID	= 1L;
	private final Class<?>		k1Class;
	private final Class<?>		k2Class;
	private final Class<?>		valueClass;

	public DoubleKeyMap() {
		k1Class = Object.class;
		k2Class = Object.class;
		valueClass = Object.class;
	}

	public DoubleKeyMap(Class<K1> k1Class, Class<K2> k2Class, Class<V> valueClass) {
		this.k1Class = k1Class;
		this.k2Class = k2Class;
		this.valueClass = valueClass;
	}

	public DoubleKeyMap(Map<K1, Map<K2, V>> other) {
		this();
		for (java.util.Map.Entry<K1, Map<K2, V>> e : other.entrySet()) {
			putAll(e.getKey(), e.getValue());
		}
	}

	public DoubleKeyMap(DoubleKeyMap<K1, K2, V> other) {
		k1Class = other.k1Class;
		k2Class = other.k2Class;
		valueClass = other.valueClass;

		for (java.util.Map.Entry<K1, Map<K2, V>> e : other.entrySet()) {
			putAll(e.getKey(), e.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	public V put(K1 key1, K2 key2, V value) {
		assert k1Class.isInstance(key1);
		assert k2Class.isInstance(key2);
		assert valueClass.isInstance(value);

		Map<K2, V> map = get(key1);
		if (map == null) {
			map = new HashMap<>();
			if (valueClass != Object.class) {
				map = Collections.checkedMap(map, (Class<K2>) k2Class, (Class<V>) valueClass);
			}
			put(key1, map);
		}
		return map.put(key2, value);
	}

	public V get(K1 key1, K2 key2) {
		Map<K2, V> map = get(key1);
		if (map == null)
			return null;

		return map.get(key2);
	}

	public boolean containsKeys(K1 key1, K2 key2) {
		Map<K2, V> map = get(key1);
		if (map == null)
			return false;

		return map.containsKey(key2);
	}

	public void putAll(K1 key1, Map<K2, V> map) {
		assert k1Class.isInstance(key1);

		for (Map.Entry<K2, V> e : map.entrySet()) {
			put(key1, e.getKey(), e.getValue());
		}
	}

	public boolean removeValue(K1 key1, K2 key2, V value) {
		assert k1Class.isInstance(key1);
		assert k2Class.isInstance(key2);
		assert valueClass.isInstance(value);

		Map<K2, V> set = get(key1);
		if (set == null) {
			return false;
		}
		boolean result = set.values()
			.remove(value);
		if (set.isEmpty())
			remove(key1);
		return result;
	}

	public V removeKey(K1 key1, K2 key2) {
		assert k1Class.isInstance(key1);
		assert k2Class.isInstance(key2);

		Map<K2, V> set = get(key1);
		if (set == null) {
			return null;
		}
		V result = set.remove(key2);
		if (set.isEmpty())
			remove(key1);
		return result;
	}

	public Iterator<Map.Entry<K2, V>> iterate(K1 key) {
		assert k1Class.isInstance(key);
		Map<K2, V> set = get(key);
		if (set == null)
			return new Iterator<Map.Entry<K2, V>>() {

				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public Map.Entry<K2, V> next() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

			};
		return set.entrySet()
			.iterator();
	}

	public Iterator<V> all() {
		return new Iterator<V>() {
			Iterator<java.util.Map.Entry<K1, Map<K2, V>>>	master	= entrySet().iterator();
			Iterator<Map.Entry<K2, V>>						current	= null;

			@Override
			public boolean hasNext() {
				if (current == null || !current.hasNext()) {
					if (master.hasNext()) {
						current = master.next()
							.getValue()
							.entrySet()
							.iterator();
						return current.hasNext();
					}
					return false;
				}
				return true;
			}

			@Override
			public V next() {
				return current.next()
					.getValue();
			}

			@Override
			public void remove() {
				current.remove();
			}

		};
	}

}
