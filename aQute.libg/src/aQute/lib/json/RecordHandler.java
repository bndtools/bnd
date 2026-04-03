package aQute.lib.json;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
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

		public Accessor(RecordComponent component, int index) throws IllegalAccessException {
			Method m = component.getAccessor();
			getter = lookup.unreflect(m);
			this.name = JSONCodec.keyword(component.getName());
			this.type = component.getGenericType();
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
		assert c.isRecord();

		MethodType constructorType = MethodType.methodType(void.class);
		int index = 0;
		for (RecordComponent component : c.getRecordComponents()) {
			constructorType = constructorType.appendParameterTypes(component.getType());
			Accessor accessor = new Accessor(component, index++);
			accessors.put(accessor.name, accessor);
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

			StringHandler.string(enc, JSONCodec.keyword(a.name));
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
		while (r.codec.isStartCharacter(c)) {

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

			if (r.codec.promiscuous && r.isEof()) {
				r.codec.fishy.incrementAndGet();
				return create(args);
			}

			throw new IllegalArgumentException(
				"Invalid character in parsing record, expected } or , but found " + (char) c);
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
