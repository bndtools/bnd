package aQute.bnd.unmodifiable;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

final class ImmutableSet<E> extends AbstractSet<E> implements Set<E>, Serializable {
	final static ImmutableSet<?>	EMPTY	= new ImmutableSet<>();
	final E[]						elements;
	final transient short[]			hash_bucket;

	@SafeVarargs
	ImmutableSet(E... elements) {
		this.elements = elements;
		this.hash_bucket = hash(elements);
	}

	private static <E> short[] hash(E[] elements) {
		int length = elements.length;
		if (length == 0) {
			return new short[1];
		}
		if (length >= (1 << Short.SIZE)) {
			throw new IllegalArgumentException("set too large: " + length);
		}
		short[] hash_bucket = new short[length * 2];
		for (int i = 0; i < length;) {
			int slot = linear_probe(elements, hash_bucket, elements[i]);
			if (slot >= 0) {
				throw new IllegalArgumentException("duplicate element: " + elements[i]);
			}
			hash_bucket[-1 - slot] = (short) ++i;
		}
		return hash_bucket;
	}

	// https://en.wikipedia.org/wiki/Linear_probing
	private static <E> int linear_probe(E[] elements, short[] hash_bucket, Object e) {
		int length = hash_bucket.length;
		for (int hash = (e.hashCode() & 0x7FFF_FFFF) % length;; hash = (hash + 1) % length) {
			int slot = Short.toUnsignedInt(hash_bucket[hash]) - 1;
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

	// Serialization support
	private static final long serialVersionUID = 1L;

	private void readObject(ObjectInputStream ois) throws InvalidObjectException {
		throw new InvalidObjectException("proxy required");
	}

	private Object writeReplace() {
		return new SerializationProxy(this);
	}

	private static final class SerializationProxy implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private transient Object[]	data;

		SerializationProxy(ImmutableSet<?> set) {
			data = set.elements;
		}

		private void writeObject(ObjectOutputStream oos) throws IOException {
			oos.defaultWriteObject();
			final Object[] local = data;
			final int length = local.length;
			oos.writeInt(length);
			for (int i = 0; i < length; i++) {
				oos.writeObject(local[i]);
			}
		}

		private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
			ois.defaultReadObject();
			final int length = ois.readInt();
			if (length < 0) {
				throw new InvalidObjectException("negative length");
			}
			final Object[] local = new Object[length];
			for (int i = 0; i < length; i++) {
				local[i] = ois.readObject();
			}
			data = local;
		}

		private Object readResolve() throws InvalidObjectException {
			try {
				final Object[] local = data;
				if (local.length == 0) {
					return EMPTY;
				}
				return new ImmutableSet<>(local);
			} catch (RuntimeException e) {
				InvalidObjectException ioe = new InvalidObjectException("invalid");
				ioe.initCause(e);
				throw ioe;
			}
		}
	}
}
