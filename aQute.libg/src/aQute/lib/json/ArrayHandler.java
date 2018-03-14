package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import aQute.lib.hex.Hex;

public class ArrayHandler extends Handler {
	Type componentType;

	ArrayHandler(@SuppressWarnings("unused") Class<?> rawClass, Type componentType) {
		this.componentType = componentType;
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {

		// Byte arrays should not be treated as arrays. We treat them
		// as hex strings

		if (object instanceof byte[]) {
			StringHandler.string(app, Hex.toHexString((byte[]) object));
			return;
		}
		app.append("[");
		app.indent();
		String del = "";
		int l = Array.getLength(object);
		for (int i = 0; i < l; i++)
			try {
				app.append(del);
				app.encode(Array.get(object, i), componentType, visited);
				del = ",";
			} catch (Exception e) {
				throw new IllegalArgumentException("[" + i + "]", e);
			}
		app.undent();
		app.append("]");
	}

	@Override
	public Object decodeArray(Decoder r) throws Exception {
		ArrayList<Object> list = new ArrayList<>();
		r.codec.parseArray(list, componentType, r);
		Object array = Array.newInstance(r.codec.getRawClass(componentType), list.size());
		int n = 0;
		for (Object o : list)
			Array.set(array, n++, o);

		return array;
	}
}
