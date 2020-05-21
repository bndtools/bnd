package aQute.lib.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

public class Sets {

	private Sets() {}

	public static <E> Set<E> of() {
		return new ImmutableSet<>();
	}

	public static <E> Set<E> of(E e1) {
		return new ImmutableSet<>(e1);
	}

	public static <E> Set<E> of(E e1, E e2) {
		return new ImmutableSet<>(e1, e2);
	}

	public static <E> Set<E> of(E e1, E e2, E e3) {
		return new ImmutableSet<>(e1, e2, e3);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4) {
		return new ImmutableSet<>(e1, e2, e3, e4);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
		return new ImmutableSet<>(e1, e2, e3, e4, e5);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
		return new ImmutableSet<>(e1, e2, e3, e4, e5, e6);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
		return new ImmutableSet<>(e1, e2, e3, e4, e5, e6, e7);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
		return new ImmutableSet<>(e1, e2, e3, e4, e5, e6, e7, e8);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
		return new ImmutableSet<>(e1, e2, e3, e4, e5, e6, e7, e8, e9);
	}

	public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
		return new ImmutableSet<>(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
	}

	@SafeVarargs
	public static <E> Set<E> of(E... elements) {
		return new ImmutableSet<>(elements);
	}

	@SuppressWarnings("unchecked")
	public static <E> Set<E> copyOf(Collection<? extends E> collection) {
		if (collection instanceof ImmutableSet) {
			return (Set<E>) collection;
		}
		E[] elements = (E[]) collection.stream()
			.distinct()
			.toArray();
		return new ImmutableSet<>(elements);
	}

	static final class ImmutableSet<E> extends AbstractSet<E> {
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

		@Override
		public Iterator<E> iterator() {
			return new ElementIterator();
		}

		@Override
		public int size() {
			return elements.length;
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

		final class ElementIterator implements Iterator<E> {
			private int index = 0;

			ElementIterator() {}

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
	}
}
