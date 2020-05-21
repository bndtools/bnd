package aQute.lib.unmodifiable;

import java.util.ListIterator;
import java.util.NoSuchElementException;

final class ImmutableListIterator<E> extends ImmutableIterator<E> implements ListIterator<E> {
	ImmutableListIterator(E[] elements) {
		super(elements);
	}

	ImmutableListIterator(E[] elements, int index) {
		super(elements, index);
	}

	@Override
	public boolean hasPrevious() {
		return index > 0;
	}

	@Override
	public E previous() {
		if (hasPrevious()) {
			return elements[--index];
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
