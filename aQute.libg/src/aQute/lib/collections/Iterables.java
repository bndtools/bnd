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

	private static class Distinct<T, R> implements Iterable<R> {
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
			Iterator<? extends T> it1 = first.iterator();
			Iterator<? extends T> it2 = second.iterator();
			it1.forEachRemaining((T t) -> {
				R r = mapper.apply(t);
				if (filter.test(r)) {
					action.accept(r);
				}
			});
			it2.forEachRemaining((T t) -> {
				R r = mapper.apply(t);
				if (filter.test(r) && !first.contains(t)) {
					action.accept(r);
				}
			});
		}

		@Override
		public Iterator<R> iterator() {
			return new Iterator<R>() {
				private final Iterator<? extends T>	it1		= first.iterator();
				private final Iterator<? extends T>	it2		= second.iterator();
				private boolean						hasNext	= false;
				private R							next;

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
						hasNext = false;
						return next;
					}
					throw new NoSuchElementException();
				}
			};
		}

		@Override
		public Spliterator<R> spliterator() {
			Spliterator<? extends T> it1 = first.spliterator();
			Spliterator<? extends T> it2 = second.spliterator();
			long est = it1.estimateSize() + it2.estimateSize();
			int characteristics = Spliterator.DISTINCT;
			if (est < 0) {
				est = Long.MAX_VALUE;
			} else {
				characteristics |= Spliterator.SIZED;
			}
			if (it1.hasCharacteristics(Spliterator.ORDERED)) {
				characteristics |= Spliterator.ORDERED;
			}
			return new AbstractSpliterator<R>(est, characteristics) {
				@Override
				public boolean tryAdvance(Consumer<? super R> action) {
					requireNonNull(action);
					if (it1.tryAdvance((T t) -> {
						R r = mapper.apply(t);
						if (filter.test(r)) {
							action.accept(r);
						}
					})) {
						return true;
					}
					return it2.tryAdvance((T t) -> {
						R r = mapper.apply(t);
						if (filter.test(r) && !first.contains(t)) {
							action.accept(r);
						}
					});
				}
			};
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

	private static class IterableEnumeration<T, R> implements Iterable<R> {
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
			return new Iterator<R>() {
				private boolean	hasNext	= false;
				private R		next;

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
						hasNext = false;
						return next;
					}
					throw new NoSuchElementException();
				}
			};
		}

		@Override
		public Spliterator<R> spliterator() {
			consume();
			return new AbstractSpliterator<R>(Long.MAX_VALUE, Spliterator.ORDERED) {
				@Override
				public boolean tryAdvance(Consumer<? super R> action) {
					requireNonNull(action);
					if (enumeration.hasMoreElements()) {
						T t = enumeration.nextElement();
						R r = mapper.apply(t);
						if (filter.test(r)) {
							action.accept(r);
						}
						return true;
					}
					return false;
				}
			};
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
