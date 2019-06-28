package aQute.lib.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObjectHandler extends Handler {
	@SuppressWarnings("rawtypes")
	final Class		rawClass;
	final Field		fields[];
	final Type		types[];
	final Object	defaults[];
	final Field		extra;

	ObjectHandler(JSONCodec codec, Class<?> c) throws Exception {
		rawClass = c;

		List<Field> fields = new ArrayList<>();
		for (Field f : c.getFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			fields.add(f);
		}

		this.fields = fields.toArray(new Field[0]);

		// Sort the fields so the output is canonical
		Arrays.sort(this.fields, (o1, o2) -> o1.getName()
			.compareTo(o2.getName()));

		types = new Type[this.fields.length];
		defaults = new Object[this.fields.length];

		Field x = null;
		for (int i = 0; i < this.fields.length; i++) {
			if (this.fields[i].getName()
				.equals("__extra"))
				x = this.fields[i];
			types[i] = this.fields[i].getGenericType();
		}
		if (x != null && Map.class.isAssignableFrom(x.getType()))
			extra = x;
		else
			extra = null;

		try {
			Object template = newInstance(c);

			for (int i = 0; i < this.fields.length; i++) {
				defaults[i] = getField(this.fields[i], template);
			}
		} catch (Exception e) {
			// Ignore
		}
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws Exception {
		app.append("{");
		app.indent();
		String del = "";
		for (int i = 0; i < fields.length; i++)
			try {
				if (fields[i].getName()
					.startsWith("__"))
					continue;

				Object value = getField(fields[i], object);
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
			} catch (Exception e) {
				throw new IllegalArgumentException(fields[i].getName() + ":", e);
			}
		app.undent();
		app.append("}");
	}

	@Override
	public Object decodeObject(Decoder r) throws Exception {
		assert r.current() == '{';
		@SuppressWarnings("unchecked")
		Object targetObject = newInstance(rawClass);

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
				if (value != null || !r.codec.ignorenull) {
					if (Modifier.isFinal(f.getModifiers()))
						throw new IllegalArgumentException("Field " + f + " is final");

					setField(f, targetObject, value);
				}
			} else {
				// No field, but may extra is defined
				if (extra == null) {
					if (r.strict)
						throw new IllegalArgumentException("No such field " + key);
					Object value = r.codec.decode(null, r);
					r.getExtra()
						.put(rawClass.getName() + "." + key, value);
				} else {

					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>) getField(extra, targetObject);
					if (map == null) {
						map = new LinkedHashMap<>();
						setField(extra, targetObject, map);
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

			throw new IllegalArgumentException(
				"Invalid character in parsing object, expected } or , but found " + (char) c);
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
