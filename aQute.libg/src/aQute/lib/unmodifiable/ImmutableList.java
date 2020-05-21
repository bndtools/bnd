package aQute.lib.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

final class ImmutableList<E> extends AbstractList<E> implements List<E>, RandomAccess {
	final E[] elements;

	@SafeVarargs
	ImmutableList(E... elements) {
		this.elements = requireNonNull(elements);
		for (int i = 0, len = elements.length; i < len; i++) {
			requireNonNull(elements[i]);
		}
	}

	private ImmutableList(E[] elements, int fromIndex, int toIndex) {
		this.elements = Arrays.copyOfRange(elements, fromIndex, toIndex);
	}

	@Override
	public Iterator<E> iterator() {
		return new ImmutableIterator<>(elements);
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ImmutableListIterator<>(elements);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new ImmutableListIterator<>(elements, index);
	}

	@Override
	public int size() {
		return elements.length;
	}

	@Override
	public E get(int index) {
		return elements[index];
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if ((fromIndex == 0) && (toIndex == elements.length)) {
			return this;
		}
		if ((fromIndex < 0) || (toIndex > elements.length) || (fromIndex > toIndex)) {
			throw new IndexOutOfBoundsException();
		}
		return new ImmutableList<>(elements, fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(elements, elements.length);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] array) {
		int length = elements.length;
		if (length > array.length) {
			return (T[]) Arrays.copyOf(elements, length, array.getClass());
		}
		System.arraycopy(elements, 0, array, 0, length);
		if (length < array.length) {
			array[length] = null;
		}
		return array;
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int i, E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E set(int i, E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sort(Comparator<? super E> comparator) {
		throw new UnsupportedOperationException();
	}
}
