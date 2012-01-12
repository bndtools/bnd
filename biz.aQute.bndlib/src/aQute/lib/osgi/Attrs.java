package aQute.lib.osgi;

import java.util.*;

import aQute.libg.version.*;

public class Attrs implements Map<String, String> {
	private HashMap<String, String>	map;
	static Map<String, String>		EMPTY	= Collections.emptyMap();

	public void clear() {
		map.clear();
	}

	public boolean containsKey(String name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	public boolean containsKey(Object name) {
		assert name instanceof String;
		if (map == null)
			return false;

		return containsKey((String) name);
	}

	public boolean containsValue(String value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	public boolean containsValue(Object value) {
		assert value instanceof String;
		if (map == null)
			return false;

		return map.containsValue((String) value);
	}

	public Set<java.util.Map.Entry<String, String>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	public String get(Object key) {
		assert key instanceof String;
		if (map == null)
			return null;

		return map.get((String) key);
	}

	public String get(String key) {
		assert key instanceof String;
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

	public String put(String key, String value) {
		if (map == null)
			map = new HashMap<String, String>();

		return map.put(key, value);
	}

	public void putAll(Map<? extends String, ? extends String> map) {
		if (this.map == null && map.isEmpty())
			return;

		map = new HashMap<String, String>();
		this.map.putAll(map);
	}

	public String remove(Object var0) {
		assert var0 instanceof String;
		if (map == null)
			return null;

		return map.remove((String) var0);
	}

	public String remove(String var0) {
		assert var0 instanceof String;
		if (map == null)
			return null;
		return map.remove(var0);
	}

	public int size() {
		if (map == null)
			return 0;
		return map.size();
	}

	public Collection<String> values() {
		if (map == null)
			return EMPTY.values();

		return map.values();
	}

	public Version getVersion() {
		String v = get(Constants.VERSION_ATTRIBUTE);
		if ( v == null)
			return Version.LOWEST;

		return Version.parseVersion(v);
	}

}
