package aQute.lib.json;

import java.io.EOFException;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * This is a simple JSON Coder and Encoder that uses the Java type system to
 * convert data objects to JSON and JSON to (type safe) Java objects. The
 * conversion is very much driven by classes and their public fields. Generic
 * information, when present is taken into account.
 * </p>
 * Usage patterns to encode:
 *
 * <pre>
 *  JSONCoder codec = new JSONCodec(); // assert "1".equals(
 * codec.enc().to().put(1).toString()); assert "[1,2,3]".equals(
 * codec.enc().to().put(Arrays.asList(1,2,3).toString()); Map m = new HashMap();
 * m.put("a", "A"); assert "{\"a\":\"A\"}".equals(
 * codec.enc().to().put(m).toString()); static class D { public int a; } D d =
 * new D(); d.a = 41; assert "{\"a\":41}".equals(
 * codec.enc().to().put(d).toString());
 * </pre>
 *
 * It is possible to redirect the encoder to another output (default is a
 * string). See {@link Encoder#to()},{@link Encoder#to(File)},
 * {@link Encoder#to(OutputStream)}, {@link Encoder#to(Appendable)}. To reset
 * the string output call {@link Encoder#to()}.
 * <p/>
 * This Codec class can be used in a concurrent environment. The Decoders and
 * Encoders, however, must only be used in a single thread.
 * <p/>
 * Will now use hex for encoding byte arrays
 */
public class JSONCodec {
	final static String								START_CHARACTERS	= "[{\"-0123456789tfn";

	// Handlers
	private final static WeakHashMap<Type, Handler>	handlers			= new WeakHashMap<>();
	private static StringHandler					sh					= new StringHandler();
	private static BooleanHandler					bh					= new BooleanHandler();
	private static CharacterHandler					ch					= new CharacterHandler();
	private static CollectionHandler				dch					= new CollectionHandler(ArrayList.class,
		Object.class);
	private static SpecialHandler					sph					= new SpecialHandler(Pattern.class, null, null);
	private static DateHandler						sdh					= new DateHandler();
	private static FileHandler						fh					= new FileHandler();
	private static ByteArrayHandler					byteh				= new ByteArrayHandler();
	private static UUIDHandler						uuidh				= new UUIDHandler();

	boolean											ignorenull;
	Map<Type, Handler>								localHandlers		= new ConcurrentHashMap<>();

	/**
	 * Create a new Encoder with the state and appropriate API.
	 *
	 * @return an Encoder
	 */
	public Encoder enc() {
		return new Encoder(this);
	}

	/**
	 * Create a new Decoder with the state and appropriate API.
	 *
	 * @return a Decoder
	 */
	public Decoder dec() {
		return new Decoder(this);
	}

	/*
	 * Work horse encode methods, all encoding ends up here.
	 */
	void encode(Encoder app, Object object, Type type, Map<Object, Type> visited) throws Exception {

		// Get the null out of the way

		if (object == null) {
			app.append("null");
			return;
		}

		// If we have no type or the type is Object.class
		// we take the type of the object itself. Normally types
		// come from declaration sites (returns, fields, methods, etc)
		// and contain generic info.

		if (type == null || type == Object.class)
			type = object.getClass();

		// Dispatch to the handler who knows how to handle the given type.
		Handler h = getHandler(type, object.getClass());
		h.encode(app, object, visited);
	}

	/**
	 * This method figures out which handler should handle the type specific
	 * stuff. It returns a handler for each type. If no appropriate handler
	 * exists, it will create one for the given type. There are actually quite a
	 * lot of handlers since Java is not very object oriented.
	 *
	 * @param type
	 * @return a {@code Handler} appropriate for {@code type}
	 * @throws Exception
	 */
	Handler getHandler(Type type, Class<?> actual) throws Exception {

		// Use the local handlers for the common types if exist.
		Handler h = localHandlers.get(type);
		if (h != null)
			return h;

		// Use the static hard coded handlers for the common types.
		if (type == String.class)
			return sh;

		if (type == Boolean.class || type == boolean.class)
			return bh;

		if (type == byte[].class)
			return byteh;

		if (Character.class == type || char.class == type)
			return ch;

		if (Pattern.class == type)
			return sph;

		if (Date.class == type)
			return sdh;

		if (File.class == type)
			return fh;

		if (UUID.class == type)
			return uuidh;

		if (type instanceof GenericArrayType) {
			Type sub = ((GenericArrayType) type).getGenericComponentType();
			if (sub == byte.class)
				return byteh;
		}

		synchronized (handlers) {
			h = handlers.get(type);
		}

		if (h != null)
			return h;

		if (type instanceof Class) {

			Class<?> clazz = (Class<?>) type;

			if (Enum.class.isAssignableFrom(clazz))
				h = new EnumHandler(clazz);
			else if (Iterable.class.isAssignableFrom(clazz)) // A Non Generic
																// collection

				h = dch;
			else if (clazz.isArray()) // Non generic array
				h = new ArrayHandler(clazz, clazz.getComponentType());
			else if (Map.class.isAssignableFrom(clazz)) // A Non Generic map
				h = new MapHandler(clazz, Object.class, Object.class);
			else if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive())
				h = new NumberHandler(clazz);
			else {
				Method valueOf = null;
				Constructor<?> constructor = null;

				try {
					constructor = clazz.getConstructor(String.class);
				} catch (Exception e) {
					// Ignore
				}
				try {
					valueOf = clazz.getMethod("valueOf", String.class);
				} catch (Exception e) {
					// Ignore
				}
				if (constructor != null || valueOf != null)
					h = new SpecialHandler(clazz, constructor, valueOf);
				else
					h = new ObjectHandler(this, clazz); // Hmm, might not be a
														// data class ...
			}

		} else {

			// We have generic information available
			// We only support generics on Collection, Map, and arrays

			if (type instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) type;
				Type rawType = pt.getRawType();
				if (rawType instanceof Class) {
					Class<?> rawClass = (Class<?>) rawType;
					if (Iterable.class.isAssignableFrom(rawClass))
						h = new CollectionHandler(rawClass, pt.getActualTypeArguments()[0]);
					else if (Map.class.isAssignableFrom(rawClass))
						h = new MapHandler(rawClass, pt.getActualTypeArguments()[0], pt.getActualTypeArguments()[1]);
					else if (Dictionary.class.isAssignableFrom(rawClass))
						h = new MapHandler(Hashtable.class, pt.getActualTypeArguments()[0],
							pt.getActualTypeArguments()[1]);
					else
						//
						// We try to use the rawtype instead.
						//
						return getHandler(rawType, null);
				}
			} else if (type instanceof GenericArrayType) {
				GenericArrayType gat = (GenericArrayType) type;
				if (gat.getGenericComponentType() == byte[].class)
					h = byteh;
				else
					h = new ArrayHandler(getRawClass(type), gat.getGenericComponentType());
			} else if (type instanceof TypeVariable) {
				if (actual != null)
					//
					// We can save ourselves a lot of work if we have
					// an actual type (the type of the object to encode)
					//
					h = getHandler(actual, null);
				else {
					TypeVariable<?> tv = (TypeVariable<?>) type;
					Type[] bounds = tv.getBounds();
					if (bounds == null || bounds.length == 0) {
						h = new ObjectHandler(this, Object.class);
					} else {
						h = getHandler(bounds[bounds.length - 1], null);
					}
				}
			} else
				throw new IllegalArgumentException("Found a parameterized type that is not a map or collection");
		}
		synchronized (handlers) {
			// We might actually have duplicates
			// but who cares? They should be identical
			handlers.put(type, h);
		}
		return h;
	}

	Object decode(Type type, Decoder isr) throws Exception {
		int c = isr.skipWs();
		Handler h;

		if (type == null || type == Object.class) {

			// Establish default behavior when we run without
			// type information

			switch (c) {
				case '{' :
					type = LinkedHashMap.class;
					break;

				case '[' :
					type = ArrayList.class;
					break;

				case '"' :
					return parseString(isr);

				case 'n' :
					isr.expect("ull");
					return null;

				case 't' :
					isr.expect("rue");
					return true;

				case 'f' :
					isr.expect("alse");
					return false;

				case '0' :
				case '1' :
				case '2' :
				case '3' :
				case '4' :
				case '5' :
				case '6' :
				case '7' :
				case '8' :
				case '9' :
				case '-' :
					return parseNumber(isr);

				default :
					throw new IllegalArgumentException("Invalid character at begin of token: " + (char) c);
			}
		}

		h = getHandler(type, null);

		switch (c) {
			case '{' :
				return h.decodeObject(isr);

			case '[' :
				return h.decodeArray(isr);

			case '"' :
				String string = parseString(isr);
				return h.decode(isr, string);

			case 'n' :
				isr.expect("ull");
				return h.decode(isr);

			case 't' :
				isr.expect("rue");
				return h.decode(isr, Boolean.TRUE);

			case 'f' :
				isr.expect("alse");
				return h.decode(isr, Boolean.FALSE);

			case '0' :
			case '1' :
			case '2' :
			case '3' :
			case '4' :
			case '5' :
			case '6' :
			case '7' :
			case '8' :
			case '9' :
			case '-' :
				return h.decode(isr, parseNumber(isr));

			default :
				throw new IllegalArgumentException("Unexpected character in input stream: " + (char) c);
		}
	}

	String parseString(Decoder r) throws Exception {
		assert r.current() == '"';

		int c = r.next(); // skip first "

		StringBuilder sb = new StringBuilder();
		while (c != '"') {
			if (c < 0 || Character.isISOControl(c))
				throw new IllegalArgumentException("JSON strings may not contain control characters: " + r.current());

			if (c == '\\') {
				c = r.read();
				switch (c) {
					case '"' :
					case '\\' :
					case '/' :
						sb.append((char) c);
						break;

					case 'b' :
						sb.append('\b');
						break;

					case 'f' :
						sb.append('\f');
						break;
					case 'n' :
						sb.append('\n');
						break;
					case 'r' :
						sb.append('\r');
						break;
					case 't' :
						sb.append('\t');
						break;
					case 'u' :
						int a3 = hexDigit(r.read()) << 12;
						int a2 = hexDigit(r.read()) << 8;
						int a1 = hexDigit(r.read()) << 4;
						int a0 = hexDigit(r.read()) << 0;
						c = a3 + a2 + a1 + a0;
						sb.append((char) c);
						break;

					default :
						throw new IllegalArgumentException(
							"The only characters after a backslash are \", \\, b, f, n, r, t, and u but got " + c);
				}
			} else
				sb.append((char) c);

			c = r.read();
		}
		assert c == '"';
		r.read(); // skip quote
		return sb.toString();
	}

	private int hexDigit(int c) throws EOFException {
		if (c >= '0' && c <= '9')
			return c - '0';

		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;

		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;

		throw new IllegalArgumentException("Invalid hex character: " + c);
	}

	private Number parseNumber(Decoder r) throws Exception {
		StringBuilder sb = new StringBuilder();
		boolean d = false;

		if (r.current() == '-') {
			sb.append('-');
			r.read();
		}

		int c = r.current();
		if (c == '0') {
			sb.append('0');
			c = r.read();
		} else if (c >= '1' && c <= '9') {
			sb.append((char) c);
			c = r.read();

			while (c >= '0' && c <= '9') {
				sb.append((char) c);
				c = r.read();
			}
		} else
			throw new IllegalArgumentException("Expected digit");

		if (c == '.') {
			d = true;
			sb.append('.');
			c = r.read();
			while (c >= '0' && c <= '9') {
				sb.append((char) c);
				c = r.read();
			}
		}
		if (c == 'e' || c == 'E') {
			d = true;
			sb.append('e');
			c = r.read();
			if (c == '+') {
				sb.append('+');
				c = r.read();
			} else if (c == '-') {
				sb.append('-');
				c = r.read();
			}
			while (c >= '0' && c <= '9') {
				sb.append((char) c);
				c = r.read();
			}
		}
		if (d)
			return Double.parseDouble(sb.toString());
		long l = Long.parseLong(sb.toString());
		if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
			return l;
		return (int) l;
	}

	void parseArray(Collection<Object> list, Type componentType, Decoder r) throws Exception {
		assert r.current() == '[';
		int c = r.next();
		while (START_CHARACTERS.indexOf(c) >= 0) {
			Object o = decode(componentType, r);
			list.add(o);

			c = r.skipWs();
			if (c == ']')
				break;

			if (c == ',') {
				c = r.next();
				continue;
			}

			throw new IllegalArgumentException(
				"Invalid character in parsing list, expected ] or , but found " + (char) c);
		}
		assert r.current() == ']';
		r.read(); // skip closing
	}

	@SuppressWarnings("rawtypes")
	Class<?> getRawClass(Type type) {
		if (type instanceof Class)
			return (Class) type;

		if (type instanceof ParameterizedType)
			return getRawClass(((ParameterizedType) type).getRawType());

		if (type instanceof GenericArrayType) {
			Type subType = ((GenericArrayType) type).getGenericComponentType();
			Class c = getRawClass(subType);
			return Array.newInstance(c, 0)
				.getClass();
		}

		throw new IllegalArgumentException(
			"Does not support generics beyond Parameterized Type  and GenericArrayType, got " + type);
	}

	/**
	 * Ignore null values in output and input
	 *
	 * @param ignorenull
	 * @return this
	 */
	public JSONCodec setIgnorenull(boolean ignorenull) {
		this.ignorenull = ignorenull;
		return this;
	}

	public boolean isIgnorenull() {
		return ignorenull;
	}

	/**
	 * Add a new local handler
	 */

	public JSONCodec addHandler(Type type, Handler handler) {
		localHandlers.put(type, handler);
		return this;
	}

}
