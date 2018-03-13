package aQute.lib.collections;

import java.util.ArrayList;
import java.util.Iterator;

public class IteratorList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;

	public IteratorList(Iterator<? extends T> i) {
		while (i.hasNext())
			add(i.next());
	}
}
