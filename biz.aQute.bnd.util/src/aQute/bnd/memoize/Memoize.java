package aQute.bnd.memoize;

import static java.util.Objects.requireNonNull;

import java.lang.ref.Reference;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Memoizing supplier.
 * <p>
 * This type extends {@code Supplier} and adds a {@link #peek()} method as well
 * as some monadic methods.
 *
 * @param <S> Type of the value returned.
 */
public interface Memoize<S> extends Supplier<S> {
	/**
	 * Creates a supplier which memoizes the value returned by the specified
	 * supplier.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified supplier at most once to obtain a value.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param supplier The source supplier. Must not be {@code null}.
	 * @return A memoizing supplier wrapping the specified supplier.
	 */
	static <T> Memoize<T> supplier(Supplier<? extends T> supplier) {
		if (supplier instanceof MemoizingSupplier) {
			@SuppressWarnings("unchecked")
			Memoize<T> memoized = (Memoize<T>) supplier;
			return memoized;
		}
		return new MemoizingSupplier<>(supplier);
	}

	/**
	 * Creates a supplier which memoizes the value returned by the specified
	 * function applied to the specified argument.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified function applied to the specified argument at most once to
	 * obtain a value.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param function The source function. Must not be {@code null}.
	 * @param argument The argument to the source function.
	 * @return A memoizing supplier wrapping the specified function and
	 *         argument.
	 */
	static <T, R> Memoize<R> supplier(Function<? super T, ? extends R> function, T argument) {
		requireNonNull(function);
		return supplier(() -> function.apply(argument));
	}

	/**
	 * Creates a supplier which memoizes, for the specified time-to-live, the
	 * value returned by the specified supplier.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified supplier to obtain a new value if any prior obtained value is
	 * older than the specified time-to-live.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param supplier The source supplier. Must not be {@code null}.
	 * @param time_to_live The time-to-live for a value. Negative values are
	 *            treated as zero.
	 * @param unit The time unit of the time-to-live value. Must not be
	 *            {@code null}.
	 * @return A memoizing supplier wrapping the specified supplier.
	 */
	static <T> Memoize<T> refreshingSupplier(Supplier<? extends T> supplier, long time_to_live, TimeUnit unit) {
		return new RefreshingMemoizingSupplier<>(supplier, time_to_live, unit);
	}

	/**
	 * Creates a supplier which memoizes a reference object holding the value
	 * returned by the specified supplier.
	 * <p>
	 * When the returned supplier is called to get a value, if the reference
	 * object holding any previously obtained value is cleared, then the
	 * specified supplier is called to obtain a new value and the specified
	 * reference function is called to wrap the new value in a reference object.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param supplier The source supplier. Must not be {@code null}. The
	 *            supplier should not return a {@code null} value since a
	 *            cleared reference also returns {@code null}.
	 * @param reference A function which is called to wrap an object created by
	 *            the specified supplier in a reference object. This allows the
	 *            caller to control the reference type and whether a reference
	 *            queue is used. The function must not return {@code null}.
	 * @return A memoizing supplier wrapping the specified supplier.
	 */
	static <T> Memoize<T> referenceSupplier(Supplier<? extends T> supplier,
		Function<? super T, ? extends Reference<? extends T>> reference) {
		return new ReferenceMemoizingSupplier<>(supplier, reference);
	}

	/**
	 * Creates a supplier which memoizes the first value returned by the
	 * specified supplier which is accepted by the specified predicate.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified supplier to obtain a value and then call the specified
	 * predicate to ask if the value is acceptable. If the value is not accepted
	 * by the predicate, the value is not memoized before it is returned. If the
	 * value is accepted by the predicate, the value is memoized before it is
	 * returned and the specified supplier and specified predicate will no
	 * longer be called.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param supplier The source supplier. Must not be {@code null}.
	 * @param predicate The accepting predicate. Must not be {@code null}.
	 * @return A memoizing supplier wrapping the specified supplier and
	 *         specified predicate.
	 * @since 1.1
	 */
	static <T> Memoize<T> predicateSupplier(Supplier<? extends T> supplier, Predicate<? super T> predicate) {
		return new PredicateMemoizingSupplier<>(supplier, predicate);
	}

	/**
	 * Peek the memoized value, if any.
	 * <p>
	 * This method will not result in a call to the source supplier.
	 *
	 * @return The value if a value is memoized; otherwise {@code null}.
	 */
	S peek();

	/**
	 * Map this memoizing supplier to a new memoizing supplier.
	 *
	 * @param <R> Type of the value returned by the new memoizing supplier.
	 * @param mapper The function to map the value of this memoizing supplier.
	 *            Must not be {@code null}.
	 * @return A new memoizing supplier which memoizes the value returned by the
	 *         specified function.
	 */
	default <R> Memoize<R> map(Function<? super S, ? extends R> mapper) {
		requireNonNull(mapper);
		return supplier(() -> mapper.apply(get()));
	}

	/**
	 * Flat map this memoizing supplier to a new memoizing supplier.
	 *
	 * @param <R> Type of the value returned by the new memoizing supplier.
	 * @param mapper The function to flat map the value of this memoizing
	 *            supplier to a supplier. Must not be {@code null}. The returned
	 *            supplier must not be {@code null}.
	 * @return A new memoizing supplier which memoizes the value returned by the
	 *         supplier returned by the specified function.
	 */
	default <R> Memoize<R> flatMap(Function<? super S, ? extends Supplier<? extends R>> mapper) {
		requireNonNull(mapper);
		return supplier(() -> mapper.apply(get())
			.get());
	}

	/**
	 * Filter this memoizing supplier to a new memoizing supplier.
	 *
	 * @param predicate The predicate to test the value of this memoizing
	 *            supplier. Must not be {@code null}.
	 * @return A new memoizing supplier which memoizes the value returned by
	 *         this supplier if the predicate accepts the value or {@code null}
	 *         otherwise.
	 */
	default Memoize<S> filter(Predicate<? super S> predicate) {
		requireNonNull(predicate);
		return supplier(() -> {
			S value = get();
			return predicate.test(value) ? value : null;
		});
	}

	/**
	 * Call the consumer with the value of this memoizing supplier.
	 *
	 * @param consumer The consumer to accept the value of this memoizing
	 *            supplier. Must not be {@code null}.
	 * @return This memoizing supplier.
	 */
	default Memoize<S> accept(Consumer<? super S> consumer) {
		requireNonNull(consumer);
		consumer.accept(get());
		return this;
	}
}
