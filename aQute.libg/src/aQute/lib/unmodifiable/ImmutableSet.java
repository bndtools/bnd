package aQute.lib.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

final class ImmutableSet<E> extends AbstractSet<E> implements Set<E> {
	final E[] elements;

	@SafeVarargs
	ImmutableSet(E... elements) {
		this.elements = requireNonNull(elements);
		for (int i = 0, len = elements.length; i < len; i++) {
			E element = requireNonNull(elements[i]);
			for (int j = i + 1; j < len; j++) {
				if (element.equals(elements[j])) {
					throw new IllegalArgumentException("duplicate element: " + element);
				}
			}
		}
	}

	ImmutableSet(E[] elements, boolean nocheck) {
		this.elements = elements;
	}

	@Override
	public Iterator<E> iterator() {
		return new ImmutableIterator<>(elements);
	}

	@Override
	public int size() {
		return elements.length;
	}

	@Override
	public boolean contains(Object o) {
		if (o != null) {
			for (E element : elements) {
				if (o.equals(element)) {
					return true;
				}
			}
		}
		return false;
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
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Set)) {
			return false;
		}
		int len = elements.length;
		Set<?> other = (Set<?>) o;
		if (len != other.size()) {
			return false;
		}
		try {
			for (int i = 0; i < len; i++) {
				if (!other.contains(elements[i])) {
					return false;
				}
			}
		} catch (ClassCastException checkedSet) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (E element : elements) {
			hashCode += element.hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean add(E e) {
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
}
