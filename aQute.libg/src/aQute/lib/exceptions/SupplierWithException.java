package aQute.lib.exceptions;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

/**
 * Supplier interface that allows exceptions.
 *
 * @param <R> the result type
 */
@FunctionalInterface
public interface SupplierWithException<R> {
	R get() throws Exception;

	default Supplier<R> orElseThrow() {
		return () -> {
			try {
				return get();
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default Supplier<R> orElse(R orElse) {
		return () -> {
			try {
				return get();
			} catch (Exception e) {
				return orElse;
			}
		};
	}

	default Supplier<R> orElseGet(Supplier<? extends R> orElseGet) {
		requireNonNull(orElseGet);
		return () -> {
			try {
				return get();
			} catch (Exception e) {
				return orElseGet.get();
			}
		};
	}

	static <R> Supplier<R> asSupplier(SupplierWithException<R> unchecked) {
		return unchecked.orElseThrow();
	}

	static <R> Supplier<R> asSupplierOrElse(SupplierWithException<R> unchecked, R orElse) {
		return unchecked.orElse(orElse);
	}

	static <R> Supplier<R> asSupplierOrElseGet(SupplierWithException<R> unchecked, Supplier<? extends R> orElseGet) {
		return unchecked.orElseGet(orElseGet);
	}
}
