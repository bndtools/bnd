package aQute.lib.json;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import aQute.bnd.exceptions.Exceptions;

public class RecordHandler extends Handler {
	final static Lookup			lookup		= MethodHandles.lookup();

	final Map<String, Accessor>	accessors	= new TreeMap<>();
	final JSONCodec				codec;
	final MethodHandle			constructor;

	class Accessor {

		final MethodHandle	getter;
		final String		name;
		final Type			type;
		final int			index;

		public Accessor(Method m, int index) throws IllegalAccessException {
			getter = lookup.unreflect(m);
			this.name = m.getName();
			this.type = m.getGenericReturnType();
			this.index = index;
		}

		public Object get(Object object) {
			try {
				return getter.invoke(object);
			} catch (Throwable e) {
				throw Exceptions.duck(e);
			}
		}

	}

	RecordHandler(JSONCodec codec, Class<?> c) throws Exception {
		this.codec = codec;
		assert c.getSuperclass() == Record.class;
		MethodType constructorType = MethodType.methodType(void.class);
		int index = 0;
		for (Field f : c.getDeclaredFields()) {
			int modifiers = f.getModifiers();
			if (Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || !Modifier.isPrivate(modifiers))
				continue;
			try {
				String name = f.getName();
				if (name.startsWith("__") && !name.equals("__extra")) {
					continue;
				}
				Method method = c.getMethod(name);
				if (method == null || method.getReturnType() != f.getType())
					continue;

				constructorType = constructorType.appendParameterTypes(f.getType());

				Accessor accessor = new Accessor(method, index++);
				accessors.put(name, accessor);

			} catch (NoSuchMethodException nsme) {
				// ignore
			}
		}
		this.constructor = lookup.findConstructor(c, constructorType);
	}

	@Override
	public void encode(Encoder enc, Object object, Map<Object, Type> visited) throws Exception {
		enc.append("{");
		enc.indent();
		String del = "";
		for (Accessor a : accessors.values()) {
			Object value = a.get(object);
			if (value == null && codec.ignorenull)
				continue;

			enc.append(del);
			if (!del.isEmpty())
				enc.linebreak();

			StringHandler.string(enc, a.name);
			enc.append(":");
			enc.encode(value, a.type, visited);
			del = ",";
		}
		enc.undent();
		enc.append("}");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object decodeObject(Decoder r) throws Exception {
		assert r.current() == '{';
		@SuppressWarnings("unchecked")
		Object[] args = new Object[accessors.size()];
		int c = r.next();
		while (JSONCodec.START_CHARACTERS.indexOf(c) >= 0) {

			// Get key
			String key = r.codec.parseString(r);

			// Get separator
			c = r.skipWs();
			if (c != ':')
				throw new IllegalArgumentException("Expected ':' but got " + (char) c);

			c = r.next();

			Accessor a = accessors.get(key);
			Object decoded = r.codec.decode(a == null ? Object.class : a.type, r);

			if (a != null)
				args[a.index] = decoded;
			else {
				Accessor extra = accessors.get("__extra");
				if (extra != null) {
					Map<String, Object> values;
					if (args[extra.index] == null) {
						args[extra.index] = values = new LinkedHashMap<>();
					} else
						values = (Map<String, Object>) args[extra.index];
					values.put(key, decoded);
				}
			}

			c = r.skipWs();

			if (c == '}')
				break;

			if (c == ',') {
				c = r.next();
				continue;
			}

			throw new IllegalArgumentException(
				"Invalid character in parsing object, expected } or , but found " + (char) c);
		}
		assert r.current() == '}';
		r.read(); // skip closing
		return create(args);
	}

	private Object create(Object[] args) {
		try {
			return constructor.invokeWithArguments(args);
		} catch (Throwable e) {
			throw Exceptions.duck(e);
		}
	}

}
