package aQute.lib.memoize;

import java.util.function.Supplier;

public interface CloseableMemoize<S extends AutoCloseable> extends Memoize<S>, AutoCloseable {
	/**
	 * Creates an AutoClosable supplier which memoizes the AutoCloseable value
	 * returned by the specified supplier.
	 * <p>
	 * When the returned supplier is called to get a value, it will call the
	 * specified supplier at most once to obtain a value.
	 * <p>
	 * When {@code close()} is called on the returned supplier, it will call
	 * {@code close()} on the memoized value if present and remove the value.
	 * After {@code close()} is called, this supplier will return {@code null}.
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
}
