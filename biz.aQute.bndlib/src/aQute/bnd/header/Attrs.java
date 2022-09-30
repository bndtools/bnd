package aQute.bnd.header;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;

public class Attrs implements Map<String, String> {
	public enum Type {
		STRING(null, "String"),
		LONG(null, "Long"),
		VERSION(null, "Version"),
		DOUBLE(null, "Double"),
		STRINGS(STRING, "List<String>"),
		LONGS(LONG, "List<Long>"),
		VERSIONS(VERSION, "List<Version>"),
		DOUBLES(DOUBLE, "List<Double>");

		final Type		sub;
		final String	toString;

		Type(Type sub, String toString) {
			this.sub = sub;
			this.toString = toString;
		}

		@Override
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

	public interface DataType<T> {
		Type type();
	}

	public static final DataType<String>		STRING			= () -> Type.STRING;
	public static final DataType<Long>			LONG			= () -> Type.LONG;
	public static final DataType<Double>		DOUBLE			= () -> Type.DOUBLE;
	public static final DataType<Version>		VERSION			= () -> Type.VERSION;
	public static final DataType<List<String>>	LIST_STRING		= () -> Type.STRINGS;
	public static final DataType<List<Long>>	LIST_LONG		= () -> Type.LONGS;
	public static final DataType<List<Double>>	LIST_DOUBLE		= () -> Type.DOUBLES;
	public static final DataType<List<Version>>	LIST_VERSION	= () -> Type.VERSIONS;

	public static Comparator<Attrs>				COMPARATOR		= Attrs::compareTo;

	/**
	 * Pattern for List with list type
	 */
	public static final Pattern					TYPED			= Pattern
		.compile("List\\s*<\\s*(String|Version|Long|Double)\\s*>");

	private final Map<String, String>			map;
	private final Map<String, Type>				types;
	public static final Attrs					EMPTY_ATTRS		= new Attrs(Collections.emptyMap(),
		Collections.emptyMap());

	private Attrs(Map<String, String> map, Map<String, Type> types) {
		this.map = map;
		this.types = types;
	}


	public Attrs() {
		this(new LinkedHashMap<>(), new HashMap<>());
	}

	public Attrs(Attrs... attrs) {
		this();
		for (Attrs a : attrs) {
			if (a != null) {
				putAll(a);
			}
		}
	}

	// do not remove, used to make sure Attrs use this one and not the next that
	// takes a Map
	public Attrs(Attrs attrs) {
		this();
		putAll(attrs);
	}

	// This constructor is also used by reflective cloning in assertj testing
	public Attrs(Map<? extends String, ? extends String> map) {
		this();
		if (map != null) {
			putAll(map);
		}
	}

	public void putAllTyped(Map<? extends String, ? extends Object> attrs) {
		attrs.forEach(this::putTyped);
	}

	public void putTyped(String key, Object value) {

		if (value == null) {
			put(key, null);
			return;
		}

		if (!(value instanceof String)) {
			Type type;

			if (value instanceof Collection)
				value = ((Collection<?>) value).toArray();

			if (value.getClass()
				.isArray()) {
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
			if (!key.endsWith(":")) { // directives are only String type
				key += ":" + type.toString();
			}
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

	@Override
	public void clear() {
		map.clear();
		types.clear();
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

	public boolean containsValue(String value) {
		return map.containsValue(value);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof String;
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return map.entrySet();
	}

	public MapStream<String, String> stream() {
		return MapStream.of(this);
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super String> action) {
		map.forEach(action);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public String get(Object key) {
		assert key instanceof String;
		return map.get(key);
	}

	public String get(String key) {
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
		return map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	public String put(String key, Type type, String value) {
		if (key == null) {
			return null;
		}
		if ((type == null) || (type == Type.STRING)) {
			types.remove(key);
		} else {
			types.put(key, type);
		}
		return map.put(key, value);
	}

	@Override
	public String put(String key, String value) {
		if (key == null)
			return null;

		return map.put(putType(key), value);
	}

	private String putType(String key) {
		int colon = key.indexOf(':');
		if (colon >= 0) {
			String type = key.substring(colon + 1)
				.trim();
			typed_attribute: if (!type.isEmpty()) { // typed attribute
				String attribute = key.substring(0, colon)
					.trim();
				switch (type) {
					case "String" :
						types.remove(attribute);
						break;
					case "Long" :
						types.put(attribute, Type.LONG);
						break;
					case "Double" :
						types.put(attribute, Type.DOUBLE);
						break;
					case "Version" :
						types.put(attribute, Type.VERSION);
						break;
					case "List" :
					case "List<String>" :
						types.put(attribute, Type.STRINGS);
						break;
					case "List<Long>" :
						types.put(attribute, Type.LONGS);
						break;
					case "List<Double>" :
						types.put(attribute, Type.DOUBLES);
						break;
					case "List<Version>" :
						types.put(attribute, Type.VERSIONS);
						break;
					default :
						Matcher m = TYPED.matcher(type);
						if (!m.matches()) {
							break typed_attribute;
						}
						switch (m.group(1)) {
							case "String" :
								types.put(attribute, Type.STRINGS);
								break;
							case "Long" :
								types.put(attribute, Type.LONGS);
								break;
							case "Double" :
								types.put(attribute, Type.DOUBLES);
								break;
							case "Version" :
								types.put(attribute, Type.VERSIONS);
								break;
						}
						break;
				}
				return attribute;
			}
		}
		// default String type
		types.remove(key);
		return key;
	}

	public Type getType(String key) {
		Type t = types.get(key);
		if (t == null)
			return Type.STRING;
		return t;
	}

	public void putAll(Attrs attrs) {
		types.keySet()
			.removeAll(attrs.map.keySet());
		map.putAll(attrs.map);
		types.putAll(attrs.types);
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> other) {
		if (other instanceof Attrs) {
			putAll((Attrs) other);
			return;
		}
		other.forEach(this::put);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public String remove(Object var0) {
		assert var0 instanceof String;
		types.remove(var0);
		return map.remove(var0);
	}

	public String remove(String var0) {
		types.remove(var0);
		return map.remove(var0);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<String> values() {
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
		String del = "";
		for (Map.Entry<String, String> e : entrySet()) {
			sb.append(del);
			append(sb, e);
			del = ";";
		}
	}

	public void append(StringBuilder sb, Map.Entry<String, String> e) {
		append(sb, e.getKey(), e.getValue());
	}

	public String toString(String key) {
		StringBuilder sb = new StringBuilder();
		append(sb, key, get(key));
		return sb.toString();
	}

	private void append(StringBuilder sb, String key, String value) {
		sb.append(key);
		Type type = getType(key);
		if (type != Type.STRING) {
			sb.append(":")
				.append(type);
		}
		sb.append("=");
		OSGiHeader.quote(sb, value);
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

		if (!keySet().equals(other.keySet()))
			return false;

		for (String key : keySet()) {
			if (!Objects.equals(get(key), other.get(key))) {
				return false;
			}
			if (getType(key) != other.getType(key)) {
				return false;
			}
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
		if (t != type.type())
			throw new IllegalArgumentException(
				"For key " + adname + ", expected " + type.type() + " but had a " + t + ". Value is " + s);

		return (T) convert(t, s);
	}

	public static Type toType(String type) {
		if (type == null) {
			return null;
		}
		for (Type t : Type.values()) {
			if (t.toString.equals(type))
				return t;
		}
		return null;
	}

	public static Object convert(String t, String s) {
		if (s == null)
			return null;

		Type type = toType(t);
		if (type == null)
			return s;

		return convert(type, s);
	}

	public static Object convert(Type t, String s) {
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
		List<Object> list = new ArrayList<>();

		List<String> split = splitListAttribute(s);
		for (String p : split)
			list.add(convert(t.sub, p));
		return list;
	}

	static List<String> splitListAttribute(String input) throws IllegalArgumentException {
		List<String> result = new ArrayList<>();

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
					builder.setLength(0);
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

	public void mergeWith(Attrs other, boolean overwrite) {
		other.forEach((key, value) -> {
			if (overwrite || !map.containsKey(key)) {
				put(key, other.getType(key), value);
			}
		});
	}

	/**
	 * Check if a directive, if so, return directive name otherwise null
	 */
	public static String toDirective(String key) {
		if (key == null) {
			return null;
		}
		int last = key.length() - 1;
		return ((last >= 0) && (key.charAt(last) == ':')) ? key.substring(0, last) : null;
	}

	/**
	 * Predicate which returns true if the specified key is an attribute key.
	 */
	public static boolean isAttribute(String key) {
		return !isDirective(key);
	}

	/**
	 * Predicate which returns true if the specified key is a directive key.
	 */
	public static boolean isDirective(String key) {
		int last = key.length() - 1;
		return (last >= 0) && (key.charAt(last) == ':');
	}

	public static Attrs create(String key, String value) {
		Attrs attrs = new Attrs();
		attrs.put(key, value);
		return attrs;
	}

	public Attrs with(String key, String value) {
		put(key, value);
		return this;
	}

	/**
	 * Return a new Attrs that has only the attributes that match the predicate.
	 * The primary use case for this is AttributeClasses.
	 *
	 * @param predicate a predicate that returns true if the attribute must be
	 *            included in the result
	 * @return a new Attrs that can be used and modified by the caller
	 */

	public Attrs select(Predicate<String> predicate) {
		Attrs attrs = new Attrs();
		forEach((k, v) -> {
			if (predicate.test(k)) {
				attrs.put(k, this.types.get(k), v);
			}
		});
		return attrs;
	}

	/**
	 * Return a new Attributes that is sorted by key
	 *
	 * @return a sorted attributes
	 */
	public Attrs sort() {
		Attrs attrs = new Attrs();
		this.entrySet()
			.stream()
			.sorted((a, b) -> a.getKey()
				.compareTo(b.getKey()))
			.forEach(e -> attrs.put(e.getKey(), e.getValue()));
		return attrs;
	}

	public Attrs set(String k, String v) {
		put(k, v);
		return this;
	}

	public int compareTo(Attrs b) {
		Attrs a = this;
		Iterator<Map.Entry<String, String>> ia = a.entrySet()
			.iterator();
		Iterator<Map.Entry<String, String>> ib = b.entrySet()
			.iterator();
		while (ia.hasNext()) {
			if (!ib.hasNext())
				return 1;

			Entry<String, String> ea = ia.next();
			Entry<String, String> eb = ib.next();
			int n = ea.getKey()
				.compareTo(eb.getKey());
			if (n != 0)
				return n;

			n = ea.getValue()
				.compareTo(eb.getValue());
			if (n != 0)
				return n;
		}
		if (ib.hasNext()) {
			return -1;
		}
		return 0;
	}

}
