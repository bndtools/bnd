package aQute.bnd.unmodifiable;

import java.util.Iterator;
import java.util.NoSuchElementException;

class ImmutableIterator<E> implements Iterator<E> {
	final Object[]	elements;
	int				index;

	ImmutableIterator(Object[] elements) {
		this.elements = elements;
	}

	@Override
	public boolean hasNext() {
		return index < elements.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E next() {
		if (hasNext()) {
			return (E) elements[index++];
		}
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
