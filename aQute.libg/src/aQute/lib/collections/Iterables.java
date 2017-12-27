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
				private final Iterator<? extends T>	it1		= first.iterator();
				private final Iterator<? extends T>	it2		= second.iterator();
				private boolean						it1next	= true;;
				private R							next;

				public boolean hasNext() {
					if (next != null) {
						return true;
					}
					while (it1.hasNext()) {
						T t = it1.next();
						R r = mapper.apply(t);
						if (r != null) {
							next = r;
							return true;
						}
					}
					it1next = false;
					while (it2.hasNext()) {
						T t = it2.next();
						R r = mapper.apply(t);
						if ((r != null) && !first.contains(t)) {
							next = r;
							return true;
						}
					}
					return false;
				}

				public R next() {
					if (hasNext()) {
						R r = next;
						next = null;
						return r;
					}
					throw new NoSuchElementException();
				}

				public void remove() {
					if (it1next) {
						it1.remove();
					}
				}
			};
		}

		@Override
		public Spliterator<R> spliterator() {
			return new Spliterator<R>() {
				private final Spliterator<? extends T>	it1	= first.spliterator();
				private final Spliterator<? extends T>	it2	= second.spliterator();

				@Override
				public boolean tryAdvance(Consumer<? super R> action) {
					requireNonNull(action);
					if (it1.tryAdvance(t -> {
						R r = mapper.apply(t);
						if (r != null) {
							action.accept(r);
						}
					})) {
						return true;
					}
					return it2.tryAdvance(t -> {
						R r = mapper.apply(t);
						if ((r != null) && !first.contains(t)) {
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
