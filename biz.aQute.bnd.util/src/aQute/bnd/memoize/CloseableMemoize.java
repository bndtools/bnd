package aQute.bnd.memoize;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Closeable memoizing supplier.
 * <p>
 * This type extends {@link Memoize} and {@code AutoCloseable}.
 *
 * @param <S> Type of the value returned.
 */
public interface CloseableMemoize<S extends AutoCloseable> extends Memoize<S>, AutoCloseable {
	/**
	 * Creates an AutoCloseable supplier which memoizes the AutoCloseable value
	 * returned by the specified supplier.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified supplier at most once to obtain a value.
	 * <p>
	 * When {@code close()} is called on the returned supplier, it will call
	 * {@code close()} on the memoized value, if present, and dereference the
	 * value. After {@code close()} is called on the returned supplier, the
	 * {@link #get()} and {@link #accept(Consumer)} methods of the returned
	 * supplier will throw an {@code IllegalStateException}.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param supplier The source supplier. Must not be {@code null}. The
	 *            supplier should not return a {@code null} value. If the
	 *            supplier does return a {@code null} value, the returned
	 *            supplier will be marked closed and its {@link #get()} and
	 *            {@link #accept(Consumer)} methods will throw an
	 *            {@code IllegalStateException}.
	 * @return A memoized supplier wrapping the specified supplier.
	 */
	static <T extends AutoCloseable> CloseableMemoize<T> closeableSupplier(Supplier<? extends T> supplier) {
		if (supplier instanceof CloseableMemoizingSupplier) {
			@SuppressWarnings("unchecked")
			CloseableMemoize<T> memoized = (CloseableMemoize<T>) supplier;
			return memoized;
		}
		return new CloseableMemoizingSupplier<>(supplier);
	}

	/**
	 * Returns whether this memoizing supplier is closed.
	 *
	 * @return {@code true} If this memoizing supplier is closed; otherwise
	 *         {@code false}.
	 */
	boolean isClosed();

	/**
	 * Get the memoized AutoCloseable value.
	 *
	 * @return The memoized AutoCloseable value.
	 * @throws IllegalStateException If this memoizing supplier
	 *             {@link #isClosed() is closed}.
	 */
	@Override
	S get();

	/**
	 * Call the consumer with the value of this memoizing supplier.
	 * <p>
	 * This method will block closing this memoizing supplier while the consumer
	 * is executing.
	 *
	 * @param consumer The consumer to accept the value of this memoizing
	 *            supplier. Must not be {@code null}.
	 * @return This memoizing supplier.
	 * @throws IllegalStateException If this memoizing supplier
	 *             {@link #isClosed() is closed}.
	 */
	@Override
	CloseableMemoize<S> accept(Consumer<? super S> consumer);

	/**
	 * If a value is memoized, call the consumer with the value of this
	 * memoizing supplier. Otherwise do nothing.
	 * <p>
	 * This method will block closing this memoizing supplier while the consumer
	 * is executing.
	 *
	 * @param consumer The consumer to accept the value of this memoizing
	 *            supplier if a value is memoized. Must not be {@code null} if a
	 *            value is memoized.
	 * @return This memoizing supplier.
	 */
	@Override
	CloseableMemoize<S> ifPresent(Consumer<? super S> consumer);
}
