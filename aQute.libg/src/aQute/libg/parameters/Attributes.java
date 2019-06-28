package aQute.libg.parameters;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.libg.qtokens.QuotedTokenizer;

public class Attributes implements Map<String, String> {
	private static final String			ORG_OSGI_FRAMEWORK_VERSION	= "org.osgi.framework.Version";
	private static final MethodHandle	newVersion;

	static {
		MethodHandle mh;
		try {
			Class<?> versionType = Attributes.class.getClassLoader()
				.loadClass(ORG_OSGI_FRAMEWORK_VERSION);
			try {
				mh = publicLookup().findStatic(versionType, "parseVersion", methodType(versionType, String.class));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				mh = publicLookup().findConstructor(versionType, methodType(void.class, String.class));
			}
		} catch (Exception e) {
			mh = null;
		}
		newVersion = mh;
	}

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
	public static final DataType<String>		VERSION			= () -> Type.VERSION;
	public static final DataType<List<String>>	LIST_STRING		= () -> Type.STRINGS;
	public static final DataType<List<Long>>	LIST_LONG		= () -> Type.LONGS;
	public static final DataType<List<Double>>	LIST_DOUBLE		= () -> Type.DOUBLES;
	public static final DataType<List<String>>	LIST_VERSION	= () -> Type.VERSIONS;

	/**
	 * Pattern for List with list type
	 */
	private static final Pattern				TYPED			= Pattern
		.compile("List\\s*<\\s*(String|Version|Long|Double)\\s*>");

	private final Map<String, String>			map;
	private final Map<String, Type>				types;
	public static final Attributes				EMPTY_ATTRS		= new Attributes(Collections.emptyMap(),
		Collections.emptyMap());

	private Attributes(Map<String, String> map, Map<String, Type> types) {
		this.map = map;
		this.types = types;
	}

	public Attributes() {
		this(new LinkedHashMap<>(), new HashMap<>());
	}

	public Attributes(Attributes... attrs) {
		this();
		for (Attributes a : attrs) {
			if (a != null) {
				putAll(a);
			}
		}
	}

	// do not remove, used to make sur Attrs use this one and not the next that
	// takes a Map
	public Attributes(Attributes attrs) {
		this();
		putAll(attrs);
	}

	public Attributes(Map<String, String> v) {
		this();
		putAll(v);
	}

	public void putAllTyped(Map<String, Object> attrs) {

		for (Map.Entry<String, Object> entry : attrs.entrySet()) {
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
		if (member.getClass()
			.getName()
			.equals(ORG_OSGI_FRAMEWORK_VERSION))
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

	@Override
	public String put(String key, String value) {
		if (key == null)
			return null;

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
				return map.put(attribute, value);
			}
		}
		// default String type
		types.remove(key);
		return map.put(key, value);
	}

	public Type getType(String key) {
		Type t = types.get(key);
		if (t == null)
			return Type.STRING;
		return t;
	}

	public void putAll(Attributes attrs) {
		types.keySet()
			.removeAll(attrs.map.keySet());
		map.putAll(attrs.map);
		types.putAll(attrs.types);
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> other) {
		if (other instanceof Attributes) {
			putAll((Attributes) other);
			return;
		}
		for (Map.Entry<? extends String, ? extends String> e : other.entrySet()) {
			put(e.getKey(), e.getValue());
		}
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

	public void append(StringBuilder appendable) {
		for (Map.Entry<String, String> e : entrySet()) {
			appendable.append(";");
			append(appendable, e);
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
		QuotedTokenizer.quote(sb, value);
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

	public boolean isEqual(Attributes other) {
		if (this == other)
			return true;

		if (other == null || size() != other.size())
			return false;

		if (isEmpty())
			return true;

		TreeSet<String> l = new TreeSet<>(keySet());
		TreeSet<String> lo = new TreeSet<>(other.keySet());
		if (!l.equals(lo))
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
					if (newVersion != null)
						try {
							return newVersion.invoke(s);
						} catch (Error | RuntimeException e) {
							throw e;
						} catch (Throwable e) {
							// ignore
						}
					return s;
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
		List<String> result = new LinkedList<>();

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

	public void mergeWith(Attributes other, boolean override) {
		for (Map.Entry<String, String> e : other.entrySet()) {
			String key = e.getKey();
			if (override || !containsKey(key)) {
				map.put(key, e.getValue());
				Type t = other.getType(key);
				if (t != Type.STRING) {
					types.put(key, t);
				} else {
					types.remove(key);
				}
			}
		}
	}

	/**
	 * Check if a directive, if so, return directive name otherwise null
	 */
	public static String toDirective(String key) {
		if (key == null || !key.endsWith(":"))
			return null;

		return key.substring(0, key.length() - 1);
	}

	public static Attributes create(String key, String value) {
		Attributes attrs = new Attributes();
		attrs.put(key, value);
		return attrs;
	}

	public Attributes with(String key, String value) {
		put(key, value);
		return this;
	}

}
