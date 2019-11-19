package aQute.launcher.minifw;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Stream;

class IteratorEnumeration<E> implements Enumeration<E> {
	private final Iterator<E> iterator;

	IteratorEnumeration(Stream<E> stream) {
		iterator = stream.iterator();
	}

	IteratorEnumeration(Iterable<E> iterable) {
		iterator = iterable.iterator();
	}

	@Override
	public boolean hasMoreElements() {
		return iterator.hasNext();
	}

	@Override
	public E nextElement() {
		return iterator.next();
	}
}
