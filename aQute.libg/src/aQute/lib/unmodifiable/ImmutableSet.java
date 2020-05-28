package aQute.lib.unmodifiable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

final class ImmutableSet<E> extends AbstractSet<E> implements Set<E> {
	final static ImmutableSet<?>	EMPTY	= new ImmutableSet<>();
	final E[]						elements;
	final int[]						hash_bucket;

	@SafeVarargs
	ImmutableSet(E... elements) {
		this.elements = elements;
		this.hash_bucket = hash(elements);
	}

	private static <E> int[] hash(E[] elements) {
		int length = elements.length;
		if (length == 0) {
			return new int[1];
		}
		int[] hash_bucket = new int[length * 2];
		for (int i = 0; i < length;) {
			int slot = linear_probe(elements, hash_bucket, elements[i]);
			if (slot >= 0) {
				throw new IllegalArgumentException("duplicate element: " + elements[i]);
			}
			hash_bucket[-1 - slot] = ++i;
		}
		return hash_bucket;
	}

	// https://en.wikipedia.org/wiki/Linear_probing
	private static <E> int linear_probe(E[] elements, int[] hash_bucket, Object e) {
		int length = hash_bucket.length;
		for (int hash = (e.hashCode() & 0x7FFF_FFFF) % length;; hash = (hash + 1) % length) {
			int slot = hash_bucket[hash] - 1;
			if (slot < 0) { // empty
				return -1 - hash;
			}
			if (elements[slot].equals(e)) { // found
				return slot;
			}
		}
	}

	private int linear_probe(Object e) {
		return linear_probe(elements, hash_bucket, e);
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
			return linear_probe(o) >= 0;
		}
		return false;
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(elements, elements.length, Object[].class);
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
		Set<?> other = (Set<?>) o;
		if (elements.length != other.size()) {
			return false;
		}
		try {
			for (E element : elements) {
				if (!other.contains(element)) {
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
