package aQute.libg.header;

import java.util.*;

import aQute.lib.collections.*;

public class Attrs implements Map<String, String> {
	private HashMap<String, String>	map;
	static Map<String, String>		EMPTY	= Collections.emptyMap();

	public Attrs(Attrs ...attrs ) {
		for ( Attrs a : attrs) {
			if ( a != null) {
				putAll(a);
			}
		}
	}
	
	
	public void clear() {
		map.clear();
	}

	public boolean containsKey(String name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof String;
		if (map == null)
			return false;

		return map.containsKey((String) name);
	}

	public boolean containsValue(String value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Deprecated
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

	@Deprecated
	public String get(Object key) {
		assert key instanceof String;
		if (map == null)
			return null;

		return map.get((String) key);
	}

	public String get(String key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	public String get(String key, String deflt) {
		String s = get(key);
		if ( s == null)
			return deflt;
		else
			return s;
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
		if (this.map == null)
			if (map.isEmpty())
				return;
			else
				this.map = new HashMap<String, String>();
		this.map.putAll(map);
	}

	@Deprecated
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

	public String getVersion() {
		return get("version");
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}
	public void append(StringBuilder sb) {
		String del = "";
		for ( Map.Entry<String, String> e : entrySet() ) {
			sb.append(del);
			sb.append(e.getKey());
			sb.append("=");
			sb.append(e.getValue());
			del = ";";
		}
	}

	public boolean equals(Object o) {
		if ( this == o)
			return true;
		
		if ( ! (o instanceof Attrs) )
			return false;
		Attrs other = (Attrs)o;
		
		if ( size() != other.size())
			return false;
		
		if ( isEmpty() )
			return true;
		
		SortedList<String> l = new SortedList<String>(keySet());
		SortedList<String> lo = new SortedList<String>(other.keySet());
		if ( !l.equals(lo) )
			return false;
		
		for ( String key : keySet()) {
			if ( !get(key).equals(other.get(key)))
				return false;
		}
		return true;
	
	}
}
