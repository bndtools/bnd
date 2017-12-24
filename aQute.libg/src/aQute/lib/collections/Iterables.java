package aQute.lib.collections;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class Iterables {
	private Iterables() {}

	private static class Intersection<T, R> implements Iterable<R> {
		private final Collection<? extends T>			first;
		private final Iterable<? extends T>				second;
		private final Function<? super T, ? extends R>	mapper;

		Intersection(Collection<? extends T> first, Iterable<? extends T> second,
			Function<? super T, ? extends R> mapper) {
			this.first = requireNonNull(first);
			this.second = requireNonNull(second);
			this.mapper = requireNonNull(mapper);
		}

		@Override
		public Iterator<R> iterator() {
			return new Iterator<R>() {
				final Iterator<? extends T>	it1	= first.iterator();
				final Iterator<? extends T>	it2	= second.iterator();
				R	current;
				R	next;

				public boolean hasNext() {
					if (it1.hasNext()) {
						return true;
					}
					if (next != null) {
						return true;
					}
					while (it2.hasNext()) {
						R r = mapper.apply(it2.next());
						if (!first.contains(r)) {
							next = r;
							return true;
						}
					}
					return false;
				}

				public R next() {
					if (it1.hasNext()) {
						return current = mapper.apply(it1.next());
					}
					current = null;
					if (next != null) {
						R r = next;
						next = null;
						return r;
					}
					while (it2.hasNext()) {
						R r = mapper.apply(it2.next());
						if (!first.contains(r)) {
							return r;
						}
					}
					throw new NoSuchElementException();
				}

				public void remove() {
					if (current != null) {
						it1.remove();
					}
				}
			};
		}

		@Override
		public Spliterator<R> spliterator() {
			return new Spliterator<R>() {
				final Spliterator<? extends T>	it1	= first.spliterator();
				final Spliterator<? extends T>	it2	= second.spliterator();

				@Override
				public boolean tryAdvance(Consumer<? super R> action) {
					requireNonNull(action);
					if (it1.tryAdvance(t -> {
						R r = mapper.apply(t);
						action.accept(r);
					})) {
						return true;
					}
					return it2.tryAdvance(t -> {
						R r = mapper.apply(t);
						if (!first.contains(r)) {
							action.accept(r);
						}
					});
				}

				@Override
				public Spliterator<R> trySplit() {
					return null;
				}

				@Override
				public long estimateSize() {
					return it1.estimateSize() + it2.estimateSize();
				}

				@Override
				public int characteristics() {
					return it1.characteristics() & it2.characteristics();
				}

				@Override
				public Comparator<? super R> getComparator() {
					if (hasCharacteristics(Spliterator.SORTED))
						return null;
					throw new IllegalStateException();
				}
			};
		}
	}

	public static <T> Iterable<T> intersection(Collection<? extends T> first, Iterable<? extends T> second) {
		return new Intersection<T, T>(first, second, Function.identity());
	}

	public static <T, R> Iterable<R> intersection(Collection<? extends T> first, Iterable<? extends T> second,
		Function<? super T, ? extends R> mapper) {
		return new Intersection<T, R>(first, second, mapper);
	}
}
