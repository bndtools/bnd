package aQute.bnd.header;

import static aQute.bnd.osgi.Constants.DUPLICATE_MARKER;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;

import aQute.bnd.stream.MapStream;
import aQute.lib.collections.SortedList;
import aQute.service.reporter.Reporter;

public class Parameters implements Map<String, Attrs> {
	private final Map<String, Attrs>	map;
	private final boolean				allowDuplicateAttributes;

	public Parameters(boolean allowDuplicateAttributes) {
		this.allowDuplicateAttributes = allowDuplicateAttributes;
		map = new LinkedHashMap<>();
	}

	public Parameters() {
		this(false);
	}

	public Parameters(String header) {
		this(header, null, false);
	}

	public Parameters(String header, Reporter reporter) {
		this(header, reporter, false);
	}

	public Parameters(String header, Reporter reporter, boolean duplicates) {
		this(duplicates);
		OSGiHeader.parseHeader(header, reporter, this);
	}

	public Parameters(Map<String, Map<String, String>> basic) {
		this();
		MapStream.ofNullable(basic)
			.mapValue(Attrs::new)
			.forEach(map::put);
	}

	@Override
	public void clear() {
		map.clear();
	}

	public void add(String key, Attrs attrs) {
		while (containsKey(key)) {
			key += DUPLICATE_MARKER;
		}
		put(key, attrs);
	}

	public boolean containsKey(String name) {
		return map.containsKey(name);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof String;
		return map.containsKey(name);
	}

	public boolean containsValue(Attrs value) {
		return map.containsValue(value);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof Attrs;
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Attrs>> entrySet() {
		return map.entrySet();
	}

	public MapStream<String, Attrs> stream() {
		return MapStream.of(this);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public Attrs get(Object key) {
		assert key instanceof String;
		return map.get(key);
	}

	public Attrs get(String key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	public List<String> keyList() {
		return keySet().stream()
			.map(Parameters::removeDuplicateMarker)
			.collect(toList());
	}

	@Override
	public Attrs put(String key, Attrs value) {
		assert key != null;
		assert value != null;
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Attrs> map) {
		this.map.putAll(map);
	}

	public void putAllIfAbsent(Map<String, ? extends Attrs> map) {
		MapStream.of(map)
			.filterKey(key -> !containsKey(key))
			.forEachOrdered(this::put);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public Attrs remove(Object var0) {
		assert var0 instanceof String;
		return map.remove(var0);
	}

	public Attrs remove(String var0) {
		return map.remove(var0);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<Attrs> values() {
		return map.values();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		String del = "";
		for (Map.Entry<String, Attrs> s : entrySet()) {
			String key = s.getKey();
			Attrs value = s.getValue();
			sb.append(del);
			sb.append(key, 0, keyLength(key));
			if (!value.isEmpty()) {
				sb.append(';');
				value.append(sb);
			}

			del = ",";
		}
	}

	private static String removeDuplicateMarker(String key) {
		return key.substring(0, keyLength(key));
	}

	private static int keyLength(String key) {
		int i = key.length() - 1;
		while ((i >= 0) && (key.charAt(i) == DUPLICATE_MARKER)) {
			--i;
		}
		return i + 1;
	}

	@Override
	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

	public boolean isEqual(Parameters other) {
		if (this == other)
			return true;

		if (other == null || size() != other.size())
			return false;

		if (isEmpty())
			return true;

		SortedList<String> l = new SortedList<>(keySet());
		SortedList<String> lo = new SortedList<>(other.keySet());
		if (!l.isEqual(lo))
			return false;

		for (String key : keySet()) {
			Attrs value = get(key);
			Attrs valueo = other.get(key);
			if (!(value == valueo || (value != null && value.isEqual(valueo))))
				return false;
		}
		return true;
	}

	public Map<String, ? extends Map<String, String>> asMapMap() {
		return this;
	}

	/**
	 * Merge all attributes of the given parameters with this
	 */
	public void mergeWith(Parameters other, boolean override) {
		for (Map.Entry<String, Attrs> e : other.entrySet()) {
			Attrs existing = get(e.getKey());
			if (existing == null) {
				put(e.getKey(), new Attrs(e.getValue()));
			} else
				existing.mergeWith(e.getValue(), override);
		}
	}

	public boolean allowDuplicateAttributes() {
		return allowDuplicateAttributes;
	}

	public static Collector<String, Parameters, Parameters> toParameters() {
		return Collector.of(Parameters::new, Parameters::accumulator, Parameters::combiner);
	}

	private static void accumulator(Parameters p, String s) {
		OSGiHeader.parseHeader(s, null, p);
	}

	private static Parameters combiner(Parameters t, Parameters u) {
		t.mergeWith(u, true);
		return t;
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	public Map<String, Map<String, String>> toBasic() {
		return (Map) this;
	}
}
