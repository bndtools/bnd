package aQute.launcher.minifw;

import static java.util.Objects.requireNonNull;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Enumerations<E> implements Enumeration<E> {
	private final Iterator<? extends E> iterator;

	public static <E> Enumeration<E> enumeration(Stream<? extends E> stream) {
		return new Enumerations<>(stream.iterator());
	}

	public static <E> Enumeration<E> enumeration(Iterable<? extends E> iterable) {
		return new Enumerations<>(iterable.iterator());
	}

	public static <E> Stream<E> stream(Enumeration<? extends E> enumeration) {
		return StreamSupport.stream(new AbstractSpliterator<E>(Long.MAX_VALUE, Spliterator.ORDERED) {
			@Override
			public boolean tryAdvance(Consumer<? super E> action) {
				requireNonNull(action);
				if (enumeration.hasMoreElements()) {
					action.accept(enumeration.nextElement());
					return true;
				}
				return false;
			}
		}, false);
	}

	private Enumerations(Iterator<? extends E> iterator) {
		this.iterator = iterator;
	}

	@Override
	public boolean hasMoreElements() {
		return iterator.hasNext();
	}

	@Override
	public E nextElement() {
		return iterator.next();
	}
}
