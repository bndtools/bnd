package aQute.lib.memoize;

import java.util.function.Supplier;

public interface CloseableMemoize<S extends AutoCloseable> extends Memoize<S>, AutoCloseable {
	/**
	 * Creates an AutoClosable supplier which memoizes the AutoCloseable value
	 * returned by the specified supplier.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified supplier at most once to obtain a value. An
	 * IllegalStateException is called when the Memoize has been closed.
	 * <p>
	 * When {@code close()} is called on the returned supplier, it will call
	 * {@code close()} on the memoized value if present and close the value.
	 * After {@code close()} is called, this supplier will throw an
	 * IllegalStateException.
	 *
	 * @param <T> Type of the value returned by the supplier.
	 * @param supplier The source supplier. Must not be {@code null}.
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
	 * Check if this Memoize has been closed
	 *
	 * @return {@code true} if this Memoize has been closed
	 */
	boolean isClosed();

}
