package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public class CollectionHandler extends Handler {
	Class<?>	rawClass;
	Type		componentType;
	
	CollectionHandler(Class<?> rawClass, Type componentType) {
		this.componentType = componentType;
		if (rawClass.isInterface()) {
			if (rawClass.isAssignableFrom(ArrayList.class))
				rawClass = ArrayList.class;
			else if (rawClass.isAssignableFrom(LinkedList.class))
				rawClass = LinkedList.class;
			else if (rawClass.isAssignableFrom(HashSet.class))
				rawClass = HashSet.class;
			else if (rawClass.isAssignableFrom(TreeSet.class))
				rawClass = TreeSet.class;
			else if (rawClass.isAssignableFrom(Vector.class))
				rawClass = Vector.class;
			else if (rawClass.isAssignableFrom(ConcurrentLinkedQueue.class))
				rawClass = ConcurrentLinkedQueue.class;
			else if (rawClass.isAssignableFrom(CopyOnWriteArrayList.class))
				rawClass = CopyOnWriteArrayList.class;
			else if (rawClass.isAssignableFrom(CopyOnWriteArraySet.class))
				rawClass = CopyOnWriteArraySet.class;
			else
				throw new IllegalArgumentException("Unknown interface type for collection: "
						+ rawClass);
		}
		this.rawClass = rawClass;
	}

	@Override void encode(Encoder app, Object object, Map<Object, Type> visited)
			throws IOException, Exception {
		Iterable<?> collection = (Iterable<?>) object;

		app.append("[");
		String del = "";
		for (Object o : collection) {
			app.append(del);
			app.encode(o, componentType, visited);
			del = ",";
		}
		app.append("]");
	}

	@SuppressWarnings("unchecked") @Override Object decodeArray(Decoder r) throws Exception {
		Collection<Object> c = (Collection<Object>) rawClass.newInstance();
		r.codec.parseArray(c, componentType, r);
		return c;
	}
}
