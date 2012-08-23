package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class ArrayHandler extends Handler {
	Type	componentType;

	ArrayHandler(@SuppressWarnings("unused") Class< ? > rawClass, Type componentType) {
		this.componentType = componentType;
	}

	@Override
	void encode(Encoder app, Object object, Map<Object,Type> visited) throws IOException, Exception {
		app.append("[");
		String del = "";
		int l = Array.getLength(object);
		for (int i = 0; i < l; i++) {
			app.append(del);
			app.encode(Array.get(object, i), componentType, visited);
			del = ",";
		}
		app.append("]");
	}

	@Override
	Object decodeArray(Decoder r) throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		r.codec.parseArray(list, componentType, r);
		Object array = Array.newInstance(r.codec.getRawClass(componentType), list.size());
		int n = 0;
		for (Object o : list)
			Array.set(array, n++, o);

		return array;
	}
}
