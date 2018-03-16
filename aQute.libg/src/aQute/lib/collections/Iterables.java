package aQute.lib.collections;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class Iterables {
	private Iterables() {}

	private static class Distinct<T, R> implements Iterable<R> {
		private final Set<? extends T>					first;
		private final Iterable<? extends T>				second;
		private final Function<? super T, ? extends R>	mapper;

		Distinct(Set<? extends T> first, Iterable<? extends T> second, Function<? super T, ? extends R> mapper) {
			this.first = requireNonNull(first);
			this.second = requireNonNull(second);
			this.mapper = requireNonNull(mapper);
		}

		@Override
		public Iterator<R> iterator() {
			return new Iterator<R>() {
				private final Iterator<? extends T>	it1		= first.iterator();
				private final Iterator<? extends T>	it2		= second.iterator();
				private R							next	= null;

				@Override
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

				@Override
				public R next() {
					if (hasNext()) {
						R r = next;
						next = null;
						return r;
					}
					throw new NoSuchElementException();
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
					if (it1.tryAdvance((T t) -> {
						R r = mapper.apply(t);
						if (r != null) {
							action.accept(r);
						}
					})) {
						return true;
					}
					return it2.tryAdvance((T t) -> {
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
					return Spliterator.DISTINCT | Spliterator.SIZED;
				}
			};
		}
	}

	public static <T> Iterable<T> distinct(Set<? extends T> first, Iterable<? extends T> second) {
		return new Distinct<>(first, second, Function.identity());
	}

	public static <T, R> Iterable<R> distinct(Set<? extends T> first, Iterable<? extends T> second,
		Function<? super T, ? extends R> mapper) {
		return new Distinct<>(first, second, mapper);
	}
}
