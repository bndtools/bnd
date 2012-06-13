package aQute.libg.header;

import java.util.*;

import aQute.lib.collections.*;
import aQute.libg.reporter.*;

public class Parameters implements Map<String,Attrs> {
	private LinkedHashMap<String,Attrs>	map;
	static Map<String,Attrs>			EMPTY	= Collections.emptyMap();
	String								error;

	public Parameters() {}

	public Parameters(String header) {
		OSGiHeader.parseHeader(header, null, this);
	}

	public Parameters(String header, Reporter reporter) {
		OSGiHeader.parseHeader(header, reporter, this);
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(final String name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof String;
		if (map == null)
			return false;

		return map.containsKey((String) name);
	}

	public boolean containsValue(Attrs value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof Attrs;
		if (map == null)
			return false;

		return map.containsValue((Attrs) value);
	}

	public Set<java.util.Map.Entry<String,Attrs>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@SuppressWarnings("cast")
	@Deprecated
	public Attrs get(Object key) {
		assert key instanceof String;
		if (map == null)
			return null;

		return map.get((String) key);
	}

	public Attrs get(String key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	public Set<String> keySet() {
		if (map == null)
			return EMPTY.keySet();

		return map.keySet();
	}

	public Attrs put(String key, Attrs value) {
		assert key != null;
		assert value != null;

		if (map == null)
			map = new LinkedHashMap<String,Attrs>();

		return map.put(key, value);
	}

	public void putAll(Map< ? extends String, ? extends Attrs> map) {
		if (this.map == null) {
			if (map.isEmpty())
				return;
			this.map = new LinkedHashMap<String,Attrs>();
		}
		this.map.putAll(map);
	}

	public void putAllIfAbsent(Map<String, ? extends Attrs> map) {
		for (Map.Entry<String, ? extends Attrs> entry : map.entrySet()) {
			if (!containsKey(entry.getKey()))
				put(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("cast")
	@Deprecated
	public Attrs remove(Object var0) {
		assert var0 instanceof String;
		if (map == null)
			return null;

		return map.remove((String) var0);
	}

	public Attrs remove(String var0) {
		if (map == null)
			return null;
		return map.remove(var0);
	}

	public int size() {
		if (map == null)
			return 0;
		return map.size();
	}

	public Collection<Attrs> values() {
		if (map == null)
			return EMPTY.values();

		return map.values();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		String del = "";
		for (Map.Entry<String,Attrs> s : entrySet()) {
			sb.append(del);
			sb.append(s.getKey());
			if (!s.getValue().isEmpty()) {
				sb.append(';');
				s.getValue().append(sb);
			}

			del = ",";
		}
	}

	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

	public boolean isEqual(Parameters other) {
		if (this == other)
			return true;

		if (size() != other.size())
			return false;

		if (isEmpty())
			return true;

		SortedList<String> l = new SortedList<String>(keySet());
		SortedList<String> lo = new SortedList<String>(other.keySet());
		if (!l.isEqual(lo))
			return false;

		for (String key : keySet()) {
			if (!get(key).isEqual(other.get(key)))
				return false;
		}
		return true;
	}

	public Map<String, ? extends Map<String,String>> asMapMap() {
		return this;
	}
}
