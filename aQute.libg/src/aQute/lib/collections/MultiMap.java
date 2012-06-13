package aQute.lib.collections;

import java.util.*;

public class MultiMap<K, V> extends HashMap<K,List<V>> {
	private static final long	serialVersionUID	= 1L;
	final boolean				noduplicates;
	final Class< ? >			keyClass;
	final Class< ? >			valueClass;

	final Set<V>				EMPTY				= Collections.emptySet();

	public MultiMap() {
		noduplicates = false;
		keyClass = Object.class;
		valueClass = Object.class;
	}

	public MultiMap(Class<K> keyClass, Class<V> valueClass, boolean noduplicates) {
		this.noduplicates = noduplicates;
		this.keyClass = keyClass;
		this.valueClass = valueClass;
	}

	@SuppressWarnings("unchecked")
	public boolean add(K key, V value) {
		assert keyClass.isInstance(key);
		assert valueClass.isInstance(value);

		List<V> set = get(key);
		if (set == null) {
			set = new ArrayList<V>();
			if (valueClass != Object.class) {
				set = Collections.checkedList(set, (Class<V>) valueClass);
			}
			put(key, set);
		} else {
			if (noduplicates) {
				if (set.contains(value))
					return false;
			}
		}
		return set.add(value);
	}

	@SuppressWarnings("unchecked")
	public boolean addAll(K key, Collection< ? extends V> value) {
		assert keyClass.isInstance(key);
		List<V> set = get(key);
		if (set == null) {
			set = new ArrayList<V>();
			if (valueClass != Object.class) {
				set = Collections.checkedList(set, (Class<V>) valueClass);
			}
			put(key, set);
		} else if (noduplicates) {
			boolean r = false;
			for (V v : value) {
				assert valueClass.isInstance(v);
				if (!set.contains(value))
					r |= set.add(v);
			}
			return r;
		}
		return set.addAll(value);
	}

	public boolean remove(K key, V value) {
		assert keyClass.isInstance(key);
		assert valueClass.isInstance(value);

		List<V> set = get(key);
		if (set == null) {
			return false;
		}
		boolean result = set.remove(value);
		if (set.isEmpty())
			remove(key);
		return result;
	}

	public boolean removeAll(K key, Collection<V> value) {
		assert keyClass.isInstance(key);
		List<V> set = get(key);
		if (set == null) {
			return false;
		}
		boolean result = set.removeAll(value);
		if (set.isEmpty())
			remove(key);
		return result;
	}

	public Iterator<V> iterate(K key) {
		assert keyClass.isInstance(key);
		List<V> set = get(key);
		if (set == null)
			return EMPTY.iterator();
		return set.iterator();
	}

	public Iterator<V> all() {
		return new Iterator<V>() {
			Iterator<List<V>>	master	= values().iterator();
			Iterator<V>			current	= null;

			public boolean hasNext() {
				if (current == null || !current.hasNext()) {
					if (master.hasNext()) {
						current = master.next().iterator();
						return current.hasNext();
					}
					return false;
				}
				return true;
			}

			public V next() {
				return current.next();
			}

			public void remove() {
				current.remove();
			}

		};
	}

	public Map<K,V> flatten() {
		Map<K,V> map = new LinkedHashMap<K,V>();
		for (Map.Entry<K,List<V>> entry : entrySet()) {
			List<V> v = entry.getValue();
			if (v == null || v.isEmpty())
				continue;

			map.put(entry.getKey(), v.get(0));
		}
		return map;
	}

	public MultiMap<V,K> transpose() {
		MultiMap<V,K> inverted = new MultiMap<V,K>();
		for (Map.Entry<K,List<V>> entry : entrySet()) {
			K key = entry.getKey();

			List<V> value = entry.getValue();
			if (value == null)
				continue;

			for (V v : value)
				inverted.add(v, key);
		}

		return inverted;
	}
}
