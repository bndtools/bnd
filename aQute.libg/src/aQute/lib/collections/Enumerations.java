package aQute.lib.collections;

import static java.util.Objects.requireNonNull;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Enumerations {

	private Enumerations() {}

	final static class EnumerationSpliterator<T, R> implements Enumeration<R>, Consumer<T> {
		private final Spliterator<? extends T>			spliterator;
		private final Function<? super T, ? extends R>	mapper;
		private final Predicate<? super R>				filter;
		private boolean									hasNext	= false;
		private R										next;
		private T										t;

		EnumerationSpliterator(Spliterator<? extends T> spliterator, Function<? super T, ? extends R> mapper,
			Predicate<? super R> filter) {
			this.spliterator = spliterator != null ? spliterator : Spliterators.emptySpliterator();
			this.mapper = requireNonNull(mapper);
			this.filter = requireNonNull(filter);
		}

		@Override
		public boolean hasMoreElements() {
			if (hasNext) {
				return true;
			}
			while (spliterator.tryAdvance(this)) {
				R r = mapper.apply(t);
				if (filter.test(r)) {
					next = r;
					return hasNext = true;
				}
			}
			return false;
		}

		@Override
		public R nextElement() {
			if (hasMoreElements()) {
				R r = next;
				hasNext = false;
				return r;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void accept(T t) {
			this.t = t;
		}
	}

	public static <T> Enumeration<T> enumeration(Spliterator<? extends T> spliterator) {
		return new EnumerationSpliterator<>(spliterator, t -> t, t -> true);
	}

	public static <T, R> Enumeration<R> enumeration(Spliterator<? extends T> spliterator,
		Function<? super T, ? extends R> mapper) {
		return new EnumerationSpliterator<>(spliterator, mapper, r -> true);
	}

	public static <T, R> Enumeration<R> enumeration(Spliterator<? extends T> spliterator,
		Function<? super T, ? extends R> mapper, Predicate<? super R> filter) {
		return new EnumerationSpliterator<>(spliterator, mapper, filter);
	}
}
