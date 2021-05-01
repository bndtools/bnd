package aQute.bnd.unmodifiable;

import java.util.Iterator;
import java.util.NoSuchElementException;

class ImmutableIterator<E> implements Iterator<E> {
	final E[]	elements;
	int			index;

	ImmutableIterator(E[] elements) {
		this.elements = elements;
	}

	@Override
	public boolean hasNext() {
		return index < elements.length;
	}

	@Override
	public E next() {
		if (hasNext()) {
			return elements[index++];
		}
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
