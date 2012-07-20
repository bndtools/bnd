package aQute.bnd.header;

import java.util.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;

public class Attrs implements Map<String,String> {
	public enum Type {
		STRING(null), LONG(null), VERSION(null), DOUBLE(null), STRINGS(STRING), LONGS(LONG), VERSIONS(VERSION), DOUBLES(
				DOUBLE);

		Type	sub;

		Type(Type sub) {
			this.sub = sub;
		}

	}

	/**
	 * <pre>
	 * Provide-Capability ::= capability ::=
	 * name-space ::= typed-attr ::= type ::= scalar ::=
	 * capability ( ',' capability )*
	 * name-space
	 *     ( ’;’ directive | typed-attr )*
	 * symbolic-name
	 * extended ( ’:’ type ) ’=’ argument
	 * scalar | list
	 * ’String’ | ’Version’ | ’Long’
	 * list ::=
	 * ’List<’ scalar ’>’
	 * </pre>
	 */
	static String							EXTENDED	= "[\\-0-9a-zA-Z\\._]+";
	static String							SCALAR		= "String|Version|Long|Double";
	static String							LIST		= "List\\s*<\\s*(" + SCALAR + ")\\s*>";
	public static final Pattern				TYPED		= Pattern.compile("\\s*(" + EXTENDED + ")\\s*:\\s*(" + SCALAR
																+ "|" + LIST + ")\\s*");

	private LinkedHashMap<String,String>	map;
	private Map<String,Type>				types;
	static Map<String,String>				EMPTY		= Collections.emptyMap();

	public Attrs(Attrs... attrs) {
		for (Attrs a : attrs) {
			if (a != null) {
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

	@SuppressWarnings("cast")
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

	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof String;
		if (map == null)
			return false;

		return map.containsValue((String) value);
	}

	public Set<java.util.Map.Entry<String,String>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@SuppressWarnings("cast")
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
		if (s == null)
			return deflt;
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
			map = new LinkedHashMap<String,String>();

		Matcher m = TYPED.matcher(key);
		if (m.matches()) {
			key = m.group(1);
			String type = m.group(2);
			Type t = Type.STRING;

			if (type.startsWith("List")) {
				type = m.group(3);
				if ("String".equals(type))
					t = Type.STRINGS;
				else if ("Long".equals(type))
					t = Type.LONGS;
				else if ("Double".equals(type))
					t = Type.DOUBLES;
				else if ("Version".equals(type))
					t = Type.VERSIONS;
			} else {
				if ("String".equals(type))
					t = Type.STRING;
				else if ("Long".equals(type))
					t = Type.LONG;
				else if ("Double".equals(type))
					t = Type.DOUBLE;
				else if ("Version".equals(type))
					t = Type.VERSION;
			}
			if (types == null)
				types = new LinkedHashMap<String,Type>();
			types.put(key, t);

			// TODO verify value?
		}

		return map.put(key, value);
	}

	public Type getType(String key) {
		if (types == null)
			return Type.STRING;
		Type t = types.get(key);
		if (t == null)
			return Type.STRING;
		return t;
	}

	public void putAll(Map< ? extends String, ? extends String> map) {
		for (Map.Entry< ? extends String, ? extends String> e : map.entrySet())
			put(e.getKey(), e.getValue());
	}

	@SuppressWarnings("cast")
	@Deprecated
	public String remove(Object var0) {
		assert var0 instanceof String;
		if (map == null)
			return null;

		return map.remove((String) var0);
	}

	public String remove(String var0) {
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
		for (Map.Entry<String,String> e : entrySet()) {
			sb.append(del);
			sb.append(e.getKey());
			sb.append("=");
			sb.append(e.getValue());
			del = ";";
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

	public boolean isEqual(Attrs o) {
		if (this == o)
			return true;

		Attrs other = o;

		if (size() != other.size())
			return false;

		if (isEmpty())
			return true;

		SortedList<String> l = new SortedList<String>(keySet());
		SortedList<String> lo = new SortedList<String>(other.keySet());
		if (!l.isEqual(lo))
			return false;

		for (String key : keySet()) {
			if (!get(key).equals(other.get(key)))
				return false;
		}
		return true;

	}

	public Object getTyped(String adname) {
		String s = get(adname);
		if (s == null)
			return null;

		Type t = getType(adname);
		return convert(t, s);
	}

	private Object convert(Type t, String s) {
		if (t.sub == null) {
			switch (t) {
				case STRING :
					return s;
				case LONG :
					return Long.parseLong(s.trim());
				case VERSION :
					return Version.parseVersion(s);
				case DOUBLE :
					return Double.parseDouble(s.trim());
					
				case DOUBLES :
				case LONGS :
				case STRINGS :
				case VERSIONS :
					// Cannot happen since the sub is null
					return null;
			}
			return null;
		}
		List<Object> list = new ArrayList<Object>();
		String split[] = s.split("\\s*\\(\\?!\\),\\s*");
		for (String p : split) {
			p = p.replaceAll("\\\\", "");
			list.add(convert(t.sub, p));
		}
		return list;
	}
}
