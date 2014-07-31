package aQute.bnd.header;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;
import aQute.bnd.version.*;

public class Attrs implements Map<String,String> {
	public interface DataType<T> {
		Type type();
	}

	public static DataType<String>			STRING			= new DataType<String>() {

																public Type type() {
																	return Type.STRING;
																}
															};
	public static DataType<Long>			LONG			= new DataType<Long>() {

																public Type type() {
																	return Type.LONG;
																}
															};	;
	public static DataType<Double>			DOUBLE			= new DataType<Double>() {

																public Type type() {
																	return Type.DOUBLE;
																}
															};	;
	public static DataType<Version>			VERSION			= new DataType<Version>() {

																public Type type() {
																	return Type.VERSION;
																}
															};	;
	public static DataType<List<String>>	LIST_STRING		= new DataType<List<String>>() {

																public Type type() {
																	return Type.STRINGS;
																}
															};	;
	public static DataType<List<Long>>		LIST_LONG		= new DataType<List<Long>>() {

																public Type type() {
																	return Type.LONGS;
																}
															};	;
	public static DataType<List<Double>>	LIST_DOUBLE		= new DataType<List<Double>>() {

																public Type type() {
																	return Type.DOUBLES;
																}
															};	;
	public static DataType<List<Version>>	LIST_VERSION	= new DataType<List<Version>>() {

																public Type type() {
																	return Type.VERSIONS;
																}
															};	;

	public enum Type {
		STRING(null, "String"), LONG(null, "Long"), VERSION(null, "Version"), DOUBLE(null, "Double"), STRINGS(STRING,
				"List<String>"), LONGS(LONG, "List<Long>"), VERSIONS(VERSION, "List<Version>"), DOUBLES(DOUBLE,
				"List<Double>");

		Type	sub;
		String	toString;

		Type(Type sub, String toString) {
			this.sub = sub;
			this.toString = toString;
		}

		public String toString() {
			return toString;
		}

		public Type plural() {
			switch (this) {
				case DOUBLE :
					return DOUBLES;

				case LONG :
					return LONGS;
				case STRING :
					return STRINGS;
				case VERSION :
					return VERSIONS;
				default :
					return null;
			}
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
	static String				EXTENDED	= "[\\-0-9a-zA-Z\\._]+";
	static String				SCALAR		= "String|Version|Long|Double";
	static String				LIST		= "List\\s*<\\s*(" + SCALAR + ")\\s*>";
	public static final Pattern	TYPED		= Pattern.compile("\\s*(" + EXTENDED + ")\\s*:\\s*(" + SCALAR + "|" + LIST
													+ ")\\s*");

	private Map<String,String>	map;
	private Map<String,Type>	types;
	static Map<String,String>	EMPTY		= Collections.emptyMap();
	public static Attrs			EMPTY_ATTRS	= new Attrs();
	static {
		EMPTY_ATTRS.map = Collections.emptyMap();
	}

	public Attrs() {}

	public Attrs(Attrs... attrs) {
		for (Attrs a : attrs) {
			if (a != null) {
				putAll(a);
				if (a.types != null)
					types.putAll(a.types);
			}
		}
	}

	public void putAllTyped(Map<String,Object> attrs) {

		for (Map.Entry<String,Object> entry : attrs.entrySet()) {
			Object value = entry.getValue();
			String key = entry.getKey();
			putTyped(key, value);

		}
	}

	public void putTyped(String key, Object value) {

		if (value == null) {
			put(key, null);
			return;
		}

		if (!(value instanceof String)) {
			Type type;

			if (value instanceof Collection)
				value = ((Collection< ? >) value).toArray();

			if (value.getClass().isArray()) {
				type = Type.STRINGS;
				int l = Array.getLength(value);
				StringBuilder sb = new StringBuilder();
				String del = "";
				boolean first = true;
				for (int i = 0; i < l; i++) {

					Object member = Array.get(value, i);
					if (member == null) {
						// TODO What do we do with null members?
						continue;
					} else if (first) {
						type = getObjectType(member).plural();
						first = true;
					}
					sb.append(del);
					int n = sb.length();
					sb.append(member);
					while (n < sb.length()) {
						char c = sb.charAt(n);
						if (c == '\\' || c == ',') {
							sb.insert(n, '\\');
							n++;
						}
						n++;
					}

					del = ",";
				}
				value = sb;
			} else {
				type = getObjectType(value);
			}
			key += ":" + type.toString();
		}
		put(key, value.toString());

	}

	private Type getObjectType(Object member) {
		if (member instanceof Double || member instanceof Float)
			return Type.DOUBLE;
		if (member instanceof Number)
			return Type.LONG;
		if (member instanceof Version)
			return Type.VERSION;

		return Type.STRING;
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
		if (key == null)
			return null;

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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		try {
			String del = "";
			for (Map.Entry<String,String> e : entrySet()) {
				sb.append(del);
				sb.append(e.getKey());

				if (types != null) {
					Type type = types.get(e.getKey());
					if (type != null) {
						sb.append(":").append(type);
					}
				}
				sb.append("=");
				Processor.quote(sb, e.getValue());
				del = ";";
			}
		}
		catch (Exception e) {
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

	public boolean isEqual(Attrs other) {
		if (this == other)
			return true;

		if (other == null || size() != other.size())
			return false;

		if (isEmpty())
			return true;

		TreeSet<String> l = new TreeSet<String>(keySet());
		TreeSet<String> lo = new TreeSet<String>(other.keySet());
		if (!l.equals(lo))
			return false;

		for (String key : keySet()) {
			String value = get(key);
			String valueo = other.get(key);
			if (!(value == valueo || (value != null && value.equals(valueo))))
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

	@SuppressWarnings("unchecked")
	public <T> T getTyped(DataType<T> type, String adname) {
		String s = get(adname);
		if (s == null)
			return null;

		Type t = getType(adname);
		if ( t != type.type())
			throw new IllegalArgumentException("For key " + adname + ", expected " + type.type() + " but had a " + t +". Value is " + s);
		
		return (T) convert(t, s);
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

		List<String> split = splitListAttribute(s);
		for (String p : split)
			list.add(convert(t.sub, p));
		return list;
	}

	static List<String> splitListAttribute(String input) throws IllegalArgumentException {
		List<String> result = new LinkedList<String>();

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '\\' :
					i++;
					if (i >= input.length())
						throw new IllegalArgumentException("Trailing blackslash in multi-valued attribute value");
					c = input.charAt(i);
					builder.append(c);
					break;
				case ',' :
					result.add(builder.toString());
					builder = new StringBuilder();
					break;
				default :
					builder.append(c);
					break;
			}
		}
		result.add(builder.toString());
		return result;
	}
	
	/**
	 * Merge the attributes
	 */

	public void mergeWith(Attrs other, boolean override) {
		for ( Map.Entry<String,String> e: other.entrySet()) {
			String local = get( e.getKey());
			if ( override || local == null )
				put(e.getKey(), e.getValue());
		}		
	}

}
