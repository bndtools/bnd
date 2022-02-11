package aQute.bnd.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

final class ImmutableList<E> extends AbstractList<E> implements List<E>, RandomAccess, Serializable {
	final static ImmutableList<?>	EMPTY	= new ImmutableList<>();
	final Object[]					elements;

	ImmutableList(Object... elements) {
		this.elements = elements;
		for (Object element : elements) {
			requireNonNull(element);
		}
	}

	private ImmutableList(Object[] elements, int fromIndex, int toIndex) {
		this.elements = Arrays.copyOfRange(elements, fromIndex, toIndex);
	}

	@Override
	public Iterator<E> iterator() {
		return new ImmutableIterator<>(elements);
	}

	@Override
	public Spliterator<E> spliterator() {
		return Spliterators.spliterator(elements, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
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

	@SuppressWarnings("unchecked")
	@Override
	public E get(int index) {
		return (E) elements[index];
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if ((fromIndex == 0) && (toIndex == elements.length)) {
			return this;
		}
		if ((fromIndex < 0) || (toIndex > elements.length) || (fromIndex > toIndex)) {
			throw new IndexOutOfBoundsException(
				"size " + elements.length + ", fromIndex " + fromIndex + ", toIndex " + toIndex);
		}
		return new ImmutableList<>(elements, fromIndex, toIndex);
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) >= 0;
	}

	@Override
	public int indexOf(Object o) {
		if (o != null) {
			Object[] elements = this.elements;
			for (int index = 0, end = elements.length; index < end; index++) {
				if (o.equals(elements[index])) {
					return index;
				}
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		if (o != null) {
			Object[] elements = this.elements;
			for (int index = elements.length - 1; index >= 0; index--) {
				if (o.equals(elements[index])) {
					return index;
				}
			}
		}
		return -1;
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

	@SuppressWarnings("unchecked")
	@Override
	public void forEach(Consumer<? super E> action) {
		requireNonNull(action);
		Object[] elements = this.elements;
		for (int index = 0, end = elements.length; index < end; index++) {
			action.accept((E) elements[index]);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof List)) {
			return false;
		}
		Iterator<?> iter = ((List<?>) o).iterator();
		for (Object element : elements) {
			if (!iter.hasNext() || !element.equals(iter.next())) {
				return false;
			}
		}
		return !iter.hasNext();
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		for (Object element : elements) {
			hashCode = 31 * hashCode + element.hashCode();
		}
		return hashCode;
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

		SerializationProxy(ImmutableList<?> list) {
			data = list.elements;
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
				return new ImmutableList<>(local);
			} catch (RuntimeException e) {
				InvalidObjectException ioe = new InvalidObjectException("invalid");
				ioe.initCause(e);
				throw ioe;
			}
		}
	}
}
