package aQute.bnd.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

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

	@SuppressWarnings("unchecked")
	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		requireNonNull(action);
		Object[] elements = this.elements;
		for (int end = elements.length; index < end;) {
			action.accept((E) elements[index++]);
		}
	}
}
