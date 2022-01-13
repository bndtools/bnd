package aQute.lib.collections;

import static java.util.Collections.emptyEnumeration;
import static java.util.Objects.requireNonNull;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Iterables {
	private Iterables() {}

	final static class Distinct<T, R> implements Iterable<R> {
		private final Set<? extends T>					first;
		private final Iterable<? extends T>				second;
		private final Function<? super T, ? extends R>	mapper;
		private final Predicate<? super R>				filter;

		Distinct(Set<? extends T> first, Iterable<? extends T> second, Function<? super T, ? extends R> mapper,
			Predicate<? super R> filter) {
			this.first = requireNonNull(first);
			this.second = requireNonNull(second);
			this.mapper = requireNonNull(mapper);
			this.filter = requireNonNull(filter);
		}

		@Override
		public void forEach(Consumer<? super R> action) {
			requireNonNull(action);
			for (T t : first) {
				R r = mapper.apply(t);
				if (filter.test(r)) {
					action.accept(r);
				}
			}
			for (T t : second) {
				R r = mapper.apply(t);
				if (filter.test(r) && !first.contains(t)) {
					action.accept(r);
				}
			}
		}

		@Override
		public Iterator<R> iterator() {
			return new DistinctIterator();
		}

		final class DistinctIterator implements Iterator<R> {
			private final Iterator<? extends T>	it1;
			private final Iterator<? extends T>	it2;
			private boolean						hasNext	= false;
			private R							next;

			DistinctIterator() {
				this.it1 = first.iterator();
				this.it2 = second.iterator();
			}

			@Override
			public boolean hasNext() {
				if (hasNext) {
					return true;
				}
				while (it1.hasNext()) {
					T t = it1.next();
					R r = mapper.apply(t);
					if (filter.test(r)) {
						next = r;
						return hasNext = true;
					}
				}
				while (it2.hasNext()) {
					T t = it2.next();
					R r = mapper.apply(t);
					if (filter.test(r) && !first.contains(t)) {
						next = r;
						return hasNext = true;
					}
				}
				return false;
			}

			@Override
			public R next() {
				if (hasNext()) {
					R r = next;
					hasNext = false;
					return r;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void forEachRemaining(Consumer<? super R> action) {
				requireNonNull(action);
				if (hasNext) {
					R r = next;
					action.accept(r);
					hasNext = false;
				}
				while (it1.hasNext()) {
					T t = it1.next();
					R r = mapper.apply(t);
					if (filter.test(r)) {
						action.accept(r);
					}
				}
				while (it2.hasNext()) {
					T t = it2.next();
					R r = mapper.apply(t);
					if (filter.test(r) && !first.contains(t)) {
						action.accept(r);
					}
				}
			}
		}

		@Override
		public Spliterator<R> spliterator() {
			return new DistinctSpliterator();
		}

		final class DistinctSpliterator extends AbstractSpliterator<R> implements Consumer<T> {
			private Spliterator<? extends T>	it2;
			private Spliterator<? extends T>	it1;
			private T							t;

			DistinctSpliterator() {
				super(Long.MAX_VALUE, Spliterator.DISTINCT | Spliterator.ORDERED);
			}

			private Spliterator<? extends T> first() {
				Spliterator<? extends T> spliterator = it1;
				if (spliterator != null) {
					return spliterator;
				}
				return it1 = first.spliterator();
			}

			private Spliterator<? extends T> second() {
				Spliterator<? extends T> spliterator = it2;
				if (spliterator != null) {
					return spliterator;
				}
				return it2 = second.spliterator();
			}

			@Override
			public boolean tryAdvance(Consumer<? super R> action) {
				requireNonNull(action);
				for (Spliterator<? extends T> spliterator = first(); spliterator.tryAdvance(this);) {
					R r = mapper.apply(t);
					if (filter.test(r)) {
						action.accept(r);
						return true;
					}
				}
				for (Spliterator<? extends T> spliterator = second(); spliterator.tryAdvance(this);) {
					R r = mapper.apply(t);
					if (filter.test(r) && !first.contains(t)) {
						action.accept(r);
						return true;
					}
				}
				return false;
			}

			@Override
			public void forEachRemaining(Consumer<? super R> action) {
				requireNonNull(action);
				first().forEachRemaining((T t) -> {
					R r = mapper.apply(t);
					if (filter.test(r)) {
						action.accept(r);
					}
				});
				second().forEachRemaining((T t) -> {
					R r = mapper.apply(t);
					if (filter.test(r) && !first.contains(t)) {
						action.accept(r);
					}
				});
			}

			@Override
			public long estimateSize() {
				long est = first().estimateSize() + second().estimateSize();
				if (est < 0L) {
					return super.estimateSize();
				}
				return est;
			}

			@Override
			public void accept(T t) {
				this.t = t;
			}
		}
	}

	public static <T> Iterable<T> distinct(Set<? extends T> first, Iterable<? extends T> second) {
		return new Distinct<>(first, second, t -> t, t -> true);
	}

	public static <T, R> Iterable<R> distinct(Set<? extends T> first, Iterable<? extends T> second,
		Function<? super T, ? extends R> mapper) {
		return new Distinct<>(first, second, mapper, r -> true);
	}

	public static <T, R> Iterable<R> distinct(Set<? extends T> first, Iterable<? extends T> second,
		Function<? super T, ? extends R> mapper, Predicate<? super R> filter) {
		return new Distinct<>(first, second, mapper, filter);
	}

	final static class IterableEnumeration<T, R> implements Iterable<R> {
		private final Enumeration<? extends T>			enumeration;
		private final Function<? super T, ? extends R>	mapper;
		private final Predicate<? super R>				filter;
		private final AtomicBoolean						consume	= new AtomicBoolean();

		IterableEnumeration(Enumeration<? extends T> enumeration, Function<? super T, ? extends R> mapper,
			Predicate<? super R> filter) {
			this.enumeration = enumeration != null ? enumeration : emptyEnumeration();
			this.mapper = requireNonNull(mapper);
			this.filter = requireNonNull(filter);
		}

		private void consume() {
			if (consume.compareAndSet(false, true)) {
				return;
			}
			throw new IllegalStateException("enumeration already consumed");
		}

		@Override
		public void forEach(Consumer<? super R> action) {
			requireNonNull(action);
			consume();
			while (enumeration.hasMoreElements()) {
				T t = enumeration.nextElement();
				R r = mapper.apply(t);
				if (filter.test(r)) {
					action.accept(r);
				}
			}
		}

		@Override
		public Iterator<R> iterator() {
			consume();
			return new EnumerationIterator();
		}

		final class EnumerationIterator implements Iterator<R> {
			private boolean	hasNext	= false;
			private R		next;

			EnumerationIterator() {}

			@Override
			public boolean hasNext() {
				if (hasNext) {
					return true;
				}
				while (enumeration.hasMoreElements()) {
					T t = enumeration.nextElement();
					R r = mapper.apply(t);
					if (filter.test(r)) {
						next = r;
						return hasNext = true;
					}
				}
				return false;
			}

			@Override
			public R next() {
				if (hasNext()) {
					R r = next;
					hasNext = false;
					return r;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void forEachRemaining(Consumer<? super R> action) {
				requireNonNull(action);
				if (hasNext) {
					R r = next;
					action.accept(r);
					hasNext = false;
				}
				while (enumeration.hasMoreElements()) {
					T t = enumeration.nextElement();
					R r = mapper.apply(t);
					if (filter.test(r)) {
						action.accept(r);
					}
				}
			}
		}

		@Override
		public Spliterator<R> spliterator() {
			consume();
			return new EnumerationSpliterator();
		}

		final class EnumerationSpliterator extends AbstractSpliterator<R> {
			EnumerationSpliterator() {
				super(Long.MAX_VALUE, Spliterator.ORDERED);
			}

			@Override
			public boolean tryAdvance(Consumer<? super R> action) {
				requireNonNull(action);
				while (enumeration.hasMoreElements()) {
					T t = enumeration.nextElement();
					R r = mapper.apply(t);
					if (filter.test(r)) {
						action.accept(r);
						return true;
					}
				}
				return false;
			}

			@Override
			public void forEachRemaining(Consumer<? super R> action) {
				requireNonNull(action);
				while (enumeration.hasMoreElements()) {
					T t = enumeration.nextElement();
					R r = mapper.apply(t);
					if (filter.test(r)) {
						action.accept(r);
					}
				}
			}
		}
	}

	public static <T> Iterable<T> iterable(Enumeration<? extends T> enumeration) {
		return new IterableEnumeration<>(enumeration, t -> t, t -> true);
	}

	public static <T, R> Iterable<R> iterable(Enumeration<? extends T> enumeration,
		Function<? super T, ? extends R> mapper) {
		return new IterableEnumeration<>(enumeration, mapper, r -> true);
	}

	public static <T, R> Iterable<R> iterable(Enumeration<? extends T> enumeration,
		Function<? super T, ? extends R> mapper, Predicate<? super R> filter) {
		return new IterableEnumeration<>(enumeration, mapper, filter);
	}
}
