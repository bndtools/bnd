package aQute.lib.unmodifiable;

import java.util.Collection;
import java.util.List;

public class Lists {

	private Lists() {}

	public static <E> List<E> of() {
		return new ImmutableList<>();
	}

	public static <E> List<E> of(E e1) {
		return new ImmutableList<>(e1);
	}

	public static <E> List<E> of(E e1, E e2) {
		return new ImmutableList<>(e1, e2);
	}

	public static <E> List<E> of(E e1, E e2, E e3) {
		return new ImmutableList<>(e1, e2, e3);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4) {
		return new ImmutableList<>(e1, e2, e3, e4);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
		return new ImmutableList<>(e1, e2, e3, e4, e5);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
		return new ImmutableList<>(e1, e2, e3, e4, e5, e6);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
		return new ImmutableList<>(e1, e2, e3, e4, e5, e6, e7);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
		return new ImmutableList<>(e1, e2, e3, e4, e5, e6, e7, e8);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
		return new ImmutableList<>(e1, e2, e3, e4, e5, e6, e7, e8, e9);
	}

	public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
		return new ImmutableList<>(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
	}

	@SafeVarargs
	public static <E> List<E> of(E... elements) {
		return new ImmutableList<>(elements);
	}

	@SuppressWarnings("unchecked")
	public static <E> List<E> copyOf(Collection<? extends E> collection) {
		if (collection instanceof ImmutableList) {
			return (List<E>) collection;
		}
		E[] elements = (E[]) collection.toArray();
		return new ImmutableList<>(elements);
	}
}
