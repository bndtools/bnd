package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;

public class CollectionHandler extends Handler {
	Class<?>							rawClass;
	Type								componentType;
	final Supplier<Collection<Object>>	factory;

	@SuppressWarnings("unchecked")
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
				throw new IllegalArgumentException("Unknown interface type for collection: " + rawClass);
		}
		this.rawClass = rawClass;
		this.factory = (Supplier<Collection<Object>>) newInstanceFunction(rawClass);
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		Iterable<?> collection = (Iterable<?>) object;

		app.append("[");
		app.indent();
		String del = "";
		int index = 0;
		for (Object o : collection)
			try {
				app.append(del);
				if (!del.isEmpty()) {
					app.linebreak();
				}

				app.encode(o, componentType, visited);
				del = ",";
				index++;
			} catch (Exception e) {
				throw new IllegalArgumentException("[" + index + "]", e);
			}
		app.undent();
		app.append("]");
	}

	@Override
	public Object decodeArray(Decoder r) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Object> c = factory.get();
		r.codec.parseArray(c, componentType, r);
		return c;
	}
}
