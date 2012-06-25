package aQute.lib.json;

import java.lang.reflect.*;
import java.util.*;

public class ObjectHandler extends Handler {
	@SuppressWarnings("rawtypes")
	final Class		rawClass;
	final Field		fields[];
	final Type		types[];
	final Object	defaults[];
	final Field		extra;

	ObjectHandler(@SuppressWarnings("unused") JSONCodec codec, Class< ? > c) throws Exception {
		rawClass = c;
		fields = c.getFields();

		// Sort the fields so the output is canonical
		Arrays.sort(fields, new Comparator<Field>() {
			public int compare(Field o1, Field o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		types = new Type[fields.length];
		defaults = new Object[fields.length];

		Field x = null;
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getName().equals("__extra"))
				x = fields[i];
			types[i] = fields[i].getGenericType();
		}
		if (x != null && Map.class.isAssignableFrom(x.getType()))
			extra = x;
		else
			extra = null;

		try {
			Object template = c.newInstance();

			for (int i = 0; i < fields.length; i++) {
				defaults[i] = fields[i].get(template);
			}
		}
		catch (Exception e) {
			// Ignore
		}
	}

	@Override
	void encode(Encoder app, Object object, Map<Object,Type> visited) throws Exception {
		app.append("{");
		String del = "";
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getName().startsWith("__"))
				continue;

			Object value = fields[i].get(object);
			if (!app.writeDefaults) {
				if (value == defaults[i])
					continue;

				if (value != null && value.equals(defaults[i]))
					continue;
			}

			app.append(del);
			StringHandler.string(app, fields[i].getName());
			app.append(":");
			app.encode(value, types[i], visited);
			del = ",";
		}
		app.append("}");
	}

	@Override
	Object decodeObject(Decoder r) throws Exception {
		assert r.current() == '{';
		Object targetObject = rawClass.newInstance();

		int c = r.next();
		while (JSONCodec.START_CHARACTERS.indexOf(c) >= 0) {

			// Get key
			String key = r.codec.parseString(r);

			// Get separator
			c = r.skipWs();
			if (c != ':')
				throw new IllegalArgumentException("Expected ':' but got " + (char) c);

			c = r.next();

			// Get value

			Field f = getField(key);
			if (f != null) {
				// We have a field and thus a type
				Object value = r.codec.decode(f.getGenericType(), r);
				if (value != null || !r.codec.ignorenull)
					f.set(targetObject, value);
			} else {
				// No field, but may extra is defined
				if (extra == null) {
					if (r.strict)
						throw new IllegalArgumentException("No such field " + key);
					Object value = r.codec.decode(null, r);
					r.getExtra().put(rawClass.getName() + "." + key, value);
				} else {

					Map<String,Object> map = (Map<String,Object>) extra.get(targetObject);
					if (map == null) {
						map = new LinkedHashMap<String,Object>();
						extra.set(targetObject, map);
					}
					Object value = r.codec.decode(null, r);
					map.put(key, value);
				}
			}

			c = r.skipWs();

			if (c == '}')
				break;

			if (c == ',') {
				c = r.next();
				continue;
			}

			throw new IllegalArgumentException("Invalid character in parsing object, expected } or , but found "
					+ (char) c);
		}
		assert r.current() == '}';
		r.read(); // skip closing
		return targetObject;
	}

	private Field getField(String key) {
		for (int i = 0; i < fields.length; i++) {
			int n = key.compareTo(fields[i].getName());
			if (n == 0)
				return fields[i];
			if (n < 0)
				return null;
		}
		return null;
	}

}
