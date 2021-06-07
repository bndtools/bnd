package aQute.lib.collections;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class MultiMap<K, V> implements Map<K, List<V>> {
	private final boolean			noduplicates;
	private final Class<K>			keyClass;
	private final Class<V>			valueClass;
	private final Map<K, List<V>>	map;

	public MultiMap() {
		this(false);
	}

	@SuppressWarnings("unchecked")
	public MultiMap(boolean noduplicates) {
		this((Class<K>) Object.class, (Class<V>) Object.class, noduplicates);
	}

	@SuppressWarnings("unchecked")
	public MultiMap(Class<? extends K> keyClass, Class<? extends V> valueClass, boolean noduplicates) {
		this.noduplicates = noduplicates;
		this.keyClass = (Class<K>) keyClass;
		this.valueClass = (Class<V>) valueClass;
		this.map = new LinkedHashMap<>();
	}

	public MultiMap(Map<? extends K, ? extends Collection<? extends V>> other) {
		this();
		addAll(other);
	}

	public <S extends K, T extends V> MultiMap(MultiMap<S, T> other) {
		this(other.keyClass, other.valueClass, other.noduplicates);
		addAll(other);
	}

	private List<V> newValue(K key) {
		if (valueClass != Object.class) {
			return Collections.checkedList(new ArrayList<>(), valueClass);
		}
		return new ArrayList<>();
	}

	public boolean add(K key, V value) {
		assert keyClass.isInstance(key);
		assert valueClass.isInstance(value);
		List<V> values = computeIfAbsent(key, this::newValue);
		if (noduplicates && values.contains(value)) {
			return false;
		}
		return values.add(value);
	}

	public boolean addAll(K key, Collection<? extends V> value) {
		assert keyClass.isInstance(key);
		if ((value == null) || value.isEmpty()) {
			return false;
		}
		List<V> values = computeIfAbsent(key, this::newValue);
		if (noduplicates) {
			boolean added = false;
			for (V v : value) {
				assert valueClass.isInstance(v);
				if (!values.contains(v)) {
					values.add(v);
					added = true;
				}
			}
			return added;
		}
		return values.addAll(value);
	}

	public boolean addAll(Map<? extends K, ? extends Collection<? extends V>> other) {
		boolean added = false;
		for (Map.Entry<? extends K, ? extends Collection<? extends V>> entry : other.entrySet()) {
			if (addAll(entry.getKey(), entry.getValue())) {
				added = true;
			}
		}
		return added;
	}

	public boolean removeValue(K key, V value) {
		assert keyClass.isInstance(key);
		assert valueClass.isInstance(value);
		List<V> values = get(key);
		if (values == null) {
			return false;
		}
		boolean result = values.remove(value);
		if (values.isEmpty()) {
			remove(key);
		}
		return result;
	}

	public boolean removeAll(K key, Collection<? extends V> value) {
		assert keyClass.isInstance(key);
		List<V> values = get(key);
		if (values == null) {
			return false;
		}
		boolean result = values.removeAll(value);
		if (values.isEmpty()) {
			remove(key);
		}
		return result;
	}

	public Iterator<V> iterate(K key) {
		assert keyClass.isInstance(key);
		List<V> values = get(key);
		if (values == null) {
			return Collections.emptyIterator();
		}
		return values.iterator();
	}

	private Stream<V> valuesStream() {
		return values().stream()
			.filter(Objects::nonNull)
			.flatMap(List::stream);
	}

	public Iterator<V> all() {
		return valuesStream().iterator();
	}

	public Map<K, V> flatten() {
		Map<K, V> flattened = new LinkedHashMap<>();
		for (Map.Entry<K, List<V>> entry : entrySet()) {
			List<V> values = entry.getValue();
			if ((values == null) || values.isEmpty()) {
				continue;
			}
			flattened.put(entry.getKey(), values.get(0));
		}
		return flattened;
	}

	public MultiMap<V, K> transpose() {
		return transpose(false);
	}

	public MultiMap<V, K> transpose(boolean noduplicates) {
		MultiMap<V, K> transposed = new MultiMap<>(valueClass, keyClass, noduplicates);
		for (Map.Entry<K, List<V>> entry : entrySet()) {
			List<V> keys = entry.getValue();
			if (keys == null) {
				continue;
			}
			K value = entry.getKey();
			for (V key : keys) {
				if (key == null) {
					continue;
				}
				transposed.add(key, value);
			}
		}
		return transposed;
	}

	/**
	 * Return a collection with all values
	 *
	 * @return all values
	 */
	public List<V> allValues() {
		return valuesStream().collect(toList());
	}

	public static <T extends Comparable<? super T>> String format(Map<T, ? extends Collection<?>> map) {
		try (Formatter f = new Formatter()) {
			SortedList<T> keys = new SortedList<>(map.keySet());
			for (Object key : keys) {
				String name = key.toString();

				SortedList<Object> values = new SortedList<>(map.get(key), null);
				String list = vertical(40, values);
				f.format("%-39s %s", name, list);
			}
			return f.toString();
		}
	}

	static String vertical(int padding, Collection<?> used) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object s : used) {
			String name = s.toString();
			sb.append(del);
			sb.append(name);
			sb.append("\r\n");
			del = pad(padding);
		}
		if (sb.length() == 0)
			sb.append("\r\n");
		return sb.toString();
	}

	static String pad(int i) {
		StringBuilder sb = new StringBuilder();
		while (i-- > 0)
			sb.append(' ');
		return sb.toString();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public List<V> get(Object key) {
		return map.get(key);
	}

	@Override
	public List<V> put(K key, List<V> value) {
		return map.put(key, value);
	}

	@Override
	public List<V> remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<V>> m) {
		map.putAll(m);
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<List<V>> values() {
		return map.values();
	}

	@Override
	public Set<Entry<K, List<V>>> entrySet() {
		return map.entrySet();
	}

	@Override
	public List<V> getOrDefault(Object key, List<V> defaultValue) {
		return map.getOrDefault(key, defaultValue);
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super List<V>> action) {
		map.forEach(action);
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super List<V>, ? extends List<V>> function) {
		map.replaceAll(function);
	}

	@Override
	public List<V> putIfAbsent(K key, List<V> value) {
		return map.putIfAbsent(key, value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return map.remove(key, value);
	}

	@Override
	public boolean replace(K key, List<V> oldValue, List<V> newValue) {
		return map.replace(key, oldValue, newValue);
	}

	@Override
	public List<V> replace(K key, List<V> value) {
		return map.replace(key, value);
	}

	@Override
	public List<V> computeIfAbsent(K key, Function<? super K, ? extends List<V>> mappingFunction) {
		return map.computeIfAbsent(key, mappingFunction);
	}

	@Override
	public List<V> computeIfPresent(K key,
		BiFunction<? super K, ? super List<V>, ? extends List<V>> remappingFunction) {
		return map.computeIfPresent(key, remappingFunction);
	}

	@Override
	public List<V> compute(K key, BiFunction<? super K, ? super List<V>, ? extends List<V>> remappingFunction) {
		return map.compute(key, remappingFunction);
	}

	@Override
	public List<V> merge(K key, List<V> value,
		BiFunction<? super List<V>, ? super List<V>, ? extends List<V>> remappingFunction) {
		return map.merge(key, value, remappingFunction);
	}

	@Override
	public int hashCode() {
		return Objects.hash(map, keyClass, valueClass, noduplicates);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MultiMap)) {
			return false;
		}
		MultiMap<?, ?> other = (MultiMap<?, ?>) obj;
		return Objects.equals(map, other.map) && Objects.equals(keyClass, other.keyClass)
			&& Objects.equals(valueClass, other.valueClass) && (noduplicates == other.noduplicates);
	}

	@Override
	public String toString() {
		return map.toString();
	}

}
