package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import aQute.lib.codec.*;

@SuppressWarnings("unchecked") public class JSONCodec implements Codec {
	final static WeakHashMap<Class<?>, Accessor>	accessors	= new WeakHashMap<Class<?>, Accessor>();

	static class Accessor {
		final Field		fields[];
		final Type		types[];
		final Object	defaults[];
		
		Accessor(Class<?> c) throws Exception {
			fields = c.getFields();
			Arrays.sort(fields, new Comparator<Field>() {
				public int compare(Field o1, Field o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			types = new Type[fields.length];
			defaults = new Object[fields.length];
			
			Object template = c.newInstance();
			for ( int i = 0; i<fields.length; i++) {
				types[i] = fields[i].getGenericType();
				defaults[i] = fields[i].get(template);
			}
		}
	}

	class PeekReader {
		final Reader	reader;
		int				last;

		PeekReader(Reader r) {
			this.reader = r;
		}

		int get() throws IOException {
			if (last == 0) {
				int c = reader.read();
				if (c < 0)
					throw new EOFException();
				return c;
			}

			int tmp = last;
			last = 0;
			return tmp;
		}

		void unget(int c) {
			this.last = c;
		}

		int peek() throws IOException {
			if (last == 0)
				last = reader.read();
			return last;
		}

		int skipWs() throws IOException {
			int c = get();
			while (Character.isWhitespace(c))
				c = get();
			return c;
		}

		void expect(String s) throws IOException {
			for (int i = 0; i < s.length(); i++)
				check(s.charAt(i) == get(), "Expected " + s + " but got something different");
		}
	}

	public void encode(Type type, Object object, Appendable app) throws Exception {
		IdentityHashMap<Object, Type> visited = new IdentityHashMap<Object, Type>();
		encode(app, type, object, visited);
	}

	public Object decode(Reader r, Type type) throws Exception {
		PeekReader isr = new PeekReader(r);
		return decode(type, isr);
	}

	private void encode(Appendable app, Type type, Object object, Map<Object, Type> visited)
			throws Exception {
		if (object == null) {
			app.append("null");
			return;
		}

		Class<?> rawClass = toClass(type);
		Class<?> actualClass = object.getClass();

		check(rawClass.isAssignableFrom(actualClass)
				|| (rawClass.isPrimitive() && toWrapper(rawClass) == actualClass),
				"Type does not match the object's class " + rawClass + " <= " + object.getClass());

		if (object instanceof String) {
			string(app, (String) object);
			return;
		}

		if (rawClass == Boolean.class || rawClass == boolean.class) {
			app.append((Boolean) object ? "true" : "false");
			return;
		}

		if (rawClass == Character.class || rawClass == char.class) {
			string(app, object.toString());
			return;
		}

		if (rawClass.isPrimitive() || Number.class.isAssignableFrom(rawClass)
				|| Enum.class.isAssignableFrom(rawClass) || rawClass == Pattern.class) {
			app.append(object.toString());
			return;
		}

		// With the simple stuff out of the way ...
		if (visited.put(object, type) != null)
			throw new IllegalStateException("Cycle detected " + object);
		try {
			if (Collection.class.isAssignableFrom(rawClass)) {
				encodeCollection(app, type, (Collection<?>) object, visited);
				return;
			}

			if (Map.class.isAssignableFrom(rawClass)) {
				encodeMap(app, type, (Map<?, ?>) object, visited);
				return;
			}

			if (rawClass.isArray()) {
				encodeArray(app, type, object, visited);
				return;
			}

			if (rawClass == Class.class) {
				app.append(((Class<?>) object).getName());
				return;
			}

			// do Object

			app.append("{");
			
			Accessor accessor = accessors.get(rawClass);
			if ( accessor == null) {
				accessors.put(rawClass,accessor = new Accessor(rawClass));
			}

			String del = "";
			for (int i=0; i<accessor.fields.length; i++) {
				Object value = accessor.fields[i].get(object);
				if (value != accessor.defaults[i]) {
					app.append(del);
					string(app, accessor.fields[i].getName());
					app.append(":");
					encode(app, accessor.types[i], value, visited);
					del = ",";
				}
			}
			app.append("}\n");
		} finally {
			visited.remove(object);
		}
	}

	private Class<?> toWrapper(Class<?> rawClass) {
		if (rawClass == boolean.class)
			return Boolean.class;

		if (rawClass == byte.class)
			return Byte.class;

		if (rawClass == char.class)
			return Character.class;

		if (rawClass == short.class)
			return Short.class;

		if (rawClass == int.class)
			return Integer.class;

		if (rawClass == long.class)
			return Long.class;

		if (rawClass == long.class)
			return Long.class;

		if (rawClass == float.class)
			return Float.class;

		return Double.class;
	}

	private void encodeArray(Appendable app, Type type, Object object, Map<Object, Type> visited)
			throws Exception {
		Type componentType;
		if (type instanceof GenericArrayType) {
			componentType = ((GenericArrayType) type).getGenericComponentType();
		} else
			componentType = object.getClass().getComponentType();

		app.append("[");
		String del = "";
		int l = Array.getLength(object);
		for (int i = 0; i < l; i++) {
			app.append(del);
			encode(app, componentType, Array.get(object, i), visited);
			del = ", ";
		}
		app.append("]\n");

	}

	private void encodeMap(Appendable app, Type type, Map<?, ?> map, Map<Object, Type> visited)
			throws Exception {
		Type keyType = Object.class;
		Type valueType = Object.class;

		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			keyType = ptype.getActualTypeArguments()[0];
			valueType = ptype.getActualTypeArguments()[1];
		}

		app.append("{");
		String del = "";
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			app.append(del);
			Object o = entry.getKey();
			if (!(o instanceof String)) {
				StringBuilder sb = new StringBuilder();
				encode(sb, keyType, o, visited);
				string(app, sb);
			} else
				string(app, (String) o);

			encode(app, keyType, o, visited);
			app.append(":");
			encode(app, valueType, entry.getValue(), visited);
			del = ", ";
		}
		app.append("}\n");
	}

	private void encodeCollection(Appendable app, Type type, Collection<?> c,
			Map<Object, Type> visited) throws Exception {
		Type componentType = Object.class;

		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			componentType = ptype.getActualTypeArguments()[0];
		}

		app.append("[");
		String del = "";
		for (Object o : c) {
			app.append(del);
			encode(app, componentType, o, visited);
			del = ", ";
		}
		app.append("]\n");
		return;

	}

	private void string(Appendable app, CharSequence s) throws IOException {
		app.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
				app.append("\\\"");
				break;

			case '\\':
				app.append("\\\\");
				break;

			case '\b':
				app.append("\\b");
				break;

			case '\f':
				app.append("\\f");
				break;

			case '\n':
				app.append("\\n");
				break;

			case '\r':
				app.append("\\r");
				break;

			case '\t':
				app.append("\\t");
				break;

			default:
				if (Character.isISOControl(c)) {
					app.append("\\u");
					app.append("0123456789ABCDEF".charAt(0xF & (c >> 12)));
					app.append("0123456789ABCDEF".charAt(0xF & (c >> 8)));
					app.append("0123456789ABCDEF".charAt(0xF & (c >> 4)));
					app.append("0123456789ABCDEF".charAt(0xF & (c >> 0)));
				} else
					app.append(c);
			}
		}
		app.append('"');
	}

	@SuppressWarnings("rawtypes") private Object convert(Class<?> type, String s) throws Exception {

		if (type == boolean.class || type == Boolean.class)
			return Boolean.parseBoolean(s);

		if (type == byte.class || type == Byte.class)
			return Byte.parseByte(s);

		if (type == char.class || type == Character.class) {
			return s.charAt(0);
		}

		if (type == short.class || type == Short.class)
			return Short.parseShort(s);

		if (type == int.class || type == Integer.class)
			return Integer.parseInt(s);

		if (type == long.class || type == Long.class)
			return Long.parseLong(s);

		if (type == float.class || type == Float.class)
			return Float.parseFloat(s);

		if (type == double.class || type == Double.class)
			return Double.parseDouble(s);

		if (type == Pattern.class)
			return Pattern.compile(s);

		if (type == Class.class) {
			return type.getClassLoader().loadClass(s);
		}

		if (Enum.class.isAssignableFrom(type)) {
			return Enum.valueOf((Class<Enum>) type, s);
		}

		Constructor<?> cnstr = type.getConstructor(String.class);
		return cnstr.newInstance(s);
	}

	private Object decode(Type type, PeekReader isr) throws Exception {
		int c = isr.skipWs();

		switch (c) {
		case '{': {
			Class<?> rawClass = toClass(type);
			if (Map.class.isAssignableFrom(rawClass))
				return decodeMap(rawClass, type, isr);
			else if (String.class == rawClass)
				return doString(isr, '}');
			else
				return decodeObject(rawClass, isr);
		}

		case '[': {
			Class<?> rawClass = toClass(type);

			if (type instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) type;
				check(Collection.class.isAssignableFrom(rawClass),
						"must have a Collection or array here here");
				return decodeCollection(rawClass, pt.getActualTypeArguments()[0], isr);
			} else if (type instanceof GenericArrayType) {
				GenericArrayType gat = (GenericArrayType) type;
				return decodeArray(gat.getGenericComponentType(), isr);
			} else if (rawClass.isArray())
				return decodeArray(rawClass.getComponentType(), isr);
			else if (rawClass == String.class)
				return doString(isr, ']');
			else
				throw new IllegalArgumentException(
						"Got an array [] but result type is not a collection or array: " + rawClass);
		}

		case '"': {
			String s = parseString(isr);
			return convert((Class<?>) type, s);
		}

		case 't':
			isr.expect("rue");
			return Boolean.TRUE;

		case 'n':
			isr.expect("ull");
			return null;

		case 'f':
			isr.expect("alse");
			return Boolean.FALSE;

		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		case '-':
			StringBuilder sb = new StringBuilder();
			sb.append((char) c);
			c = isr.get();
			while (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
				sb.append((char) c);
				c = isr.get();
			}
			isr.unget(c);

			String s = sb.toString();
			return convert((Class<?>) type, s);

		}
		throw new IllegalArgumentException("Unexpected character in input stream: " + (char) c);
	}

	/**
	 * Gather the input until you find the the closing character making sure
	 * that new blocks are are take care of.
	 * <p>
	 * This method parses the input for a complete block so that it can be
	 * stored in a string. This allows envelopes.
	 * 
	 * @param isr
	 * @param c
	 * @return
	 * @throws IOException
	 */
	private Object doString(PeekReader isr, char close) throws IOException {
		boolean instring = false;
		int level = 1;
		StringBuilder sb = new StringBuilder();

		int c = isr.get();
		while (c > 0 && level > 0) {
			sb.append((char) c);
			if (instring)
				switch (c) {
				case '"':
					instring = true;
					break;

				case '[':
				case '{':
					level++;
					break;

				case ']':
				case '}':
					level--;
					break;
				}
			else
				switch (c) {
				case '"':
					instring = false;
					break;

				case '\\':
					sb.append((char) isr.get());
					break;
				}

			c = isr.get();
		}
		return sb.toString();
	}

	private Object decodeArray(Type type, PeekReader isr) throws Exception {
		Class<?> rawClass = toClass(type);
		int c = isr.peek();
		List<Object> result = new ArrayList<Object>();
		while (c == '"' || Character.isDigit(c) || c == '-' || c == '[' || c == '{') {
			Object member = decode(type, isr);
			result.add(member);
		}
		Object array = Array.newInstance(rawClass, result.size());

		for (int n = result.size() - 1; n >= 0; n--)
			Array.set(array, n, result.get(n));

		return result.toArray((Object[]) array);
	}

	private Object decodeCollection(Class<?> rt, Type type, PeekReader isr) throws Exception {
		if (rt.isInterface()) {
			if (rt == Collection.class || rt == List.class)
				rt = ArrayList.class;
			else if (rt == Set.class || rt == SortedSet.class)
				rt = TreeSet.class;
			else if (rt == Queue.class /* || resultType == Deque.class */)
				rt = LinkedList.class;
			else if (rt == Queue.class /* || resultType == Deque.class */)
				rt = LinkedList.class;
			else
				throw new IllegalArgumentException(
						"Unknown interface for a collection, no concrete class found: " + rt);
		}

		int c = isr.peek();

		Collection<Object> collection = (Collection<Object>) rt.newInstance();

		while (c == '"' || Character.isDigit(c) || c == '-' || c == '[' || c == '{') {
			Object member = decode(type, isr);
			collection.add(member);
			c = isr.peek();
			if (c == ',')
				c = isr.skipWs();
		}
		check(c == ']', "expected } for closing collection");
		return collection;
	}

	private Class<?> toClass(Type type) {
		if (type instanceof Class)
			return (Class<?>) type;

		if (type instanceof ParameterizedType)
			return toClass(((ParameterizedType) type).getRawType());

		if (type instanceof GenericArrayType) {
			GenericArrayType gat = (GenericArrayType) type;
			Type componentType = gat.getGenericComponentType();
			Class<?> rawComponent = toClass(componentType);
			Object array = Array.newInstance(rawComponent, 0);
			return array.getClass();
		}

		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			Type[] bounds = tv.getBounds();
			if (bounds.length == 0)
				return Object.class;
			else
				return toClass(bounds[0]);
		}
		if (type instanceof WildcardType) {
			WildcardType tv = (WildcardType) type;
			Type[] bounds = tv.getUpperBounds();
			if (bounds.length == 0)
				return Object.class;
			else
				return toClass(bounds[0]);
		}
		throw new IllegalArgumentException("Unknown type " + type);
	}

	private Object decodeMap(Class<?> clazz, Type type, PeekReader isr) throws Exception {
		if (clazz.isInterface()) {
			if (SortedMap.class.isAssignableFrom(clazz))
				clazz = TreeMap.class;
			else if (ConcurrentMap.class.isAssignableFrom(clazz))
				clazz = ConcurrentHashMap.class;
			else
				clazz = HashMap.class;
		}
		Map<Object, Object> map = (Map<Object, Object>) clazz.newInstance();
		Type keyType = Object.class;
		Type valueType = Object.class;

		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			keyType = pt.getActualTypeArguments()[0];
			valueType = pt.getActualTypeArguments()[1];
		}

		int c = isr.skipWs();

		while (c == '"') {
			Object key = parseString(isr);
			if (keyType != String.class) {
				// TODO
				key = decode(new StringReader((String) key), keyType);
			}
			check(':' == isr.skipWs(), "Expected separator");
			Object value = decode(valueType, isr);
			map.put(key, value);
			c = isr.skipWs();
			if (c != ',')
				c = isr.skipWs();
			else
				break;
		}
		check('}' == c, "Expected end of MAP: }");
		return map;
	}

	private Object decodeObject(Class<?> clazz, PeekReader isr) throws Exception {
		Object object = clazz.newInstance();

		int c = isr.skipWs();

		while (c == '"') {
			String name = parseString(isr);
			check(':' == isr.skipWs(), "Expected separator");
			Field f = clazz.getField(name);
			f.set(object, decode(f.getGenericType(), isr));
			c = isr.skipWs();
			if (c == ',')
				c = isr.skipWs();
			else
				break;
		}
		check('}' == c, "Expected end of object: }, got " + (char) c);
		return object;
	}

	private void check(boolean b, String string) {
		if (b)
			return;
		throw new IllegalArgumentException(string);
	}

	private String parseString(PeekReader r) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c = r.get();

		while (c != '\"') {
			if (c < 0 || Character.isISOControl(c))
				throw new IllegalArgumentException(
						"JSON strings may not contain control characters: " + c);

			if (c == '\\') {
				c = r.get();
				switch (c) {
				case '"':
				case '\\':
				case '/':
					sb.append(c);
					break;

				case 'b':
					sb.append('\b');
					break;

				case 'f':
					sb.append('\f');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'u':
					c = hexDigit(r.get()) << 12 + hexDigit(r.get() << 8) + hexDigit(r.get() << 4)
							+ hexDigit(r.get());
					break;

				default:
					throw new IllegalArgumentException(
							"The only characters after a backslash are \", \\, b, f, n, r, t, and u but got "
									+ c);
				}
			} else
				sb.append((char) c);

			c = r.get();
		}
		return sb.toString();
	}

	private int hexDigit(int read) throws EOFException {
		if (read >= '0' && read <= '9')
			return read - '0';

		int c = Character.toUpperCase(read);
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;

		throw new EOFException("Invalid hex character: " + c);
	}

}
