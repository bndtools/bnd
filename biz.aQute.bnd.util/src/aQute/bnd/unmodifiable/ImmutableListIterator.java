package aQute.bnd.unmodifiable;

import java.util.ListIterator;
import java.util.NoSuchElementException;

final class ImmutableListIterator<E> extends ImmutableIterator<E> implements ListIterator<E> {
	ImmutableListIterator(Object[] elements) {
		super(elements);
	}

	ImmutableListIterator(Object[] elements, int index) {
		super(elements);
		this.index = index;
		if ((index < 0) || (index > elements.length)) {
			throw new IndexOutOfBoundsException("size " + elements.length + ", index " + index);
		}
	}

	@Override
	public boolean hasPrevious() {
		return index > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E previous() {
		if (hasPrevious()) {
			return (E) elements[--index];
		}
		throw new NoSuchElementException();
	}

	@Override
	public int nextIndex() {
		return index;
	}

	@Override
	public int previousIndex() {
		return index - 1;
	}

	@Override
	public void set(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(E e) {
		throw new UnsupportedOperationException();
	}
}
