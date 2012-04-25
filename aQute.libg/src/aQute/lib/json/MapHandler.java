package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class MapHandler extends Handler {
	final Class<?>	rawClass;
	final Type		keyType;
	final Type		valueType;

	MapHandler(Class<?> rawClass, Type keyType, Type valueType) {
		this.keyType = keyType;
		this.valueType = valueType;
		if (rawClass.isInterface()) {
			if (rawClass.isAssignableFrom(HashMap.class))
				rawClass = HashMap.class;
			else if (rawClass.isAssignableFrom(TreeMap.class))
				rawClass = TreeMap.class;
			else if (rawClass.isAssignableFrom(Hashtable.class))
				rawClass = Hashtable.class;
			else if (rawClass.isAssignableFrom(LinkedHashMap.class))
				rawClass = LinkedHashMap.class;
			else
				throw new IllegalArgumentException("Unknown map interface: " + rawClass);
		}
		this.rawClass = rawClass;
	}

	@Override void encode(Encoder app, Object object, Map<Object, Type> visited)
			throws IOException, Exception {
		Map<?, ?> map = (Map<?, ?>) object;

		app.append("{");
		String del = "";
		for (Map.Entry<?, ?> e : map.entrySet()) {
			app.append(del);
			String key;
			if (e.getKey() != null && (keyType == String.class || keyType == Object.class))
				key = e.getKey().toString();
			else {
				key = app.codec.enc().put(e.getKey()).toString();
			}
			StringHandler.string(app, key);
			app.append(":");
			app.encode(e.getValue(), valueType, visited);
			del = ",";
		}
		app.append("}");
	}

	@SuppressWarnings("unchecked") @Override Object decodeObject(Decoder r) throws Exception {
		assert r.current() == '{';
		
		Map<Object, Object> map = (Map<Object, Object>) rawClass.newInstance();
		
		int c = r.next();
		while (JSONCodec.START_CHARACTERS.indexOf(c) >= 0) {
			Object key = r.codec.parseString(r);
			if ( !(keyType == null || keyType == Object.class)) {
				Handler h = r.codec.getHandler(keyType);
				key = h.decode((String)key);
			}
			
			c = r.skipWs();
			if ( c != ':')
				throw new IllegalArgumentException("Expected ':' but got " + (char) c);

			c = r.next();
			Object value = r.codec.decode(valueType, r);
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
