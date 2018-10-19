package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class MapHandler extends Handler {
	final Class<?>	rawClass;
	final Type		keyType;
	final Type		valueType;

	MapHandler(Class<?> rawClass, Type keyType, Type valueType) {

		if (rawClass != Map.class) {
			ParameterizedType type = findAncestor(rawClass, Map.class);
			this.keyType = keyType == Object.class ? resolve(type.getActualTypeArguments()[0]) : keyType;
			this.valueType = valueType == Object.class ? resolve(type.getActualTypeArguments()[1]) : valueType;
		} else {
			this.keyType = keyType;
			this.valueType = valueType;
		}

		if (rawClass.isInterface()) {
			if (rawClass.isAssignableFrom(LinkedHashMap.class))
				rawClass = LinkedHashMap.class;
			else if (rawClass.isAssignableFrom(TreeMap.class))
				rawClass = TreeMap.class;
			else if (rawClass.isAssignableFrom(Hashtable.class))
				rawClass = Hashtable.class;
			else if (rawClass.isAssignableFrom(HashMap.class))
				rawClass = HashMap.class;
			else if (rawClass.isAssignableFrom(Dictionary.class))
				rawClass = Hashtable.class;
			else
				throw new IllegalArgumentException("Unknown map interface: " + rawClass);
		}
		this.rawClass = rawClass;
	}

	private Type resolve(Type type) {
		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			Type[] bounds = tv.getBounds();
			return resolve(bounds[bounds.length - 1]);
		}
		return type;
	}

	private ParameterizedType findAncestor(Class<?> start, Class<?> target) {
		if (start == null || start == Object.class)
			return null;

		for (Type t : start.getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				if (((ParameterizedType) t).getRawType() == target)
					return (ParameterizedType) t;
			}
		}
		for (Class<?> impls : start.getInterfaces()) {
			ParameterizedType ancestor = findAncestor(impls, target);
			if (ancestor != null)
				return ancestor;
		}

		return findAncestor(start.getSuperclass(), target);
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		Map<?, ?> map = (Map<?, ?>) object;

		app.append("{");
		String del = "";
		for (Map.Entry<?, ?> e : map.entrySet())
			try {
				app.append(del);
				String key;
				if (e.getKey() != null && (keyType == String.class || keyType == Object.class))
					key = e.getKey()
						.toString();
				else {
					key = app.codec.enc()
						.put(e.getKey())
						.toString();
				}
				StringHandler.string(app, key);
				app.append(":");
				app.encode(e.getValue(), valueType, visited);
				del = ",";
			} catch (Exception ee) {
				throw new IllegalArgumentException("[\"" + e.getKey() + "\"]", ee);
			}
		app.append("}");
	}

	@Override
	public Object decodeObject(Decoder r) throws Exception {
		assert r.current() == '{';

		@SuppressWarnings("unchecked")
		Map<Object, Object> map = (Map<Object, Object>) newInstance(rawClass);

		int c = r.next();
		while (JSONCodec.START_CHARACTERS.indexOf(c) >= 0) {
			Object key = r.codec.parseString(r);
			if (!(keyType == null || keyType == Object.class)) {
				Handler h = r.codec.getHandler(keyType, null);
				key = h.decode(r, (String) key);
			}

			c = r.skipWs();
			if (c != ':')
				throw new IllegalArgumentException("Expected ':' but got " + (char) c);

			c = r.next();
			Object value = r.codec.decode(valueType, r);
			if (value != null || !r.codec.ignorenull)
				map.put(key, value);

			c = r.skipWs();

			if (c == '}')
				break;

			if (c == ',') {
				c = r.next();
				continue;
			}

			throw new IllegalArgumentException(
				"Invalid character in parsing list, expected } or , but found " + (char) c);
		}
		assert r.current() == '}';
		r.read(); // skip closing
		return map;
	}

}
