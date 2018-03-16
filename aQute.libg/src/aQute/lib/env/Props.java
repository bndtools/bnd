package aQute.lib.env;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import aQute.lib.collections.SortedList;

public class Props implements Map<String, String> {

	/**
	 */
	static String				EXTENDED	= "[\\-0-9a-zA-Z\\._]+";

	private Map<String, String>	map;
	static Map<String, String>	EMPTY		= Collections.emptyMap();
	public static Props			EMPTY_ATTRS	= new Props();

	static {
		EMPTY_ATTRS.map = Collections.emptyMap();
	}

	public Props(Props... attrs) {
		for (Props a : attrs) {
			if (a != null) {
				putAll(a);
			}
		}
	}

	@Override
	public void clear() {
		map.clear();
	}

	public boolean containsKey(String name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof String;
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	public boolean containsValue(String value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof String;
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public String get(Object key) {
		assert key instanceof String;
		if (map == null)
			return null;

		return map.get(key);
	}

	public String get(String key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	public String get(String key, String deflt) {
		String s = get(key);
		if (s == null)
			return deflt;
		return s;
	}

	@Override
	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		if (map == null)
			return EMPTY.keySet();

		return map.keySet();
	}

	@Override
	public String put(String key, String value) {
		if (key == null)
			return null;

		if (map == null)
			map = new LinkedHashMap<>();

		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> map) {
		for (Map.Entry<? extends String, ? extends String> e : map.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public String remove(Object var0) {
		assert var0 instanceof String;
		if (map == null)
			return null;

		return map.remove(var0);
	}

	public String remove(String var0) {
		if (map == null)
			return null;
		return map.remove(var0);
	}

	@Override
	public int size() {
		if (map == null)
			return 0;
		return map.size();
	}

	@Override
	public Collection<String> values() {
		if (map == null)
			return EMPTY.values();

		return map.values();
	}

	public String getVersion() {
		return get("version");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		try {
			String del = "";
			for (Map.Entry<String, String> e : entrySet()) {
				sb.append(del);
				sb.append(e.getKey());

				sb.append("=");
				Header.quote(sb, e.getValue());
				del = ";";
			}
		} catch (Exception e) {
			// Cannot happen
			e.printStackTrace();
		}
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

	public boolean isEqual(Props other) {
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
			String value = get(key);
			String valueo = other.get(key);
			if (!(value == valueo || (value != null && value.equals(valueo))))
				return false;
		}
		return true;
	}
}
