package aQute.lib.exceptions;

import java.util.function.Supplier;

/**
 * Supplier interface that allows exceptions.
 * 
 * @param <R> the result type
 */
@FunctionalInterface
public interface SupplierWithException<R> {
	R get() throws Exception;

	static <T> Supplier<T> asSupplier(SupplierWithException<T> unchecked) {
		return () -> {
			try {
				return unchecked.get();
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
