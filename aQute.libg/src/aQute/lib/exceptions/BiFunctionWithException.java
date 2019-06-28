package aQute.lib.exceptions;

import static java.util.Objects.requireNonNull;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * BiFunction interface that allows exceptions.
 *
 * @param <T> the type 1 of the argument
 * @param <U> the type 2 of the argument
 * @param <R> the result type
 */
@FunctionalInterface
public interface BiFunctionWithException<T, U, R> {
	R apply(T t, U u) throws Exception;

	default BiFunction<T, U, R> orElseThrow() {
		return (t, u) -> {
			try {
				return apply(t, u);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default BiFunction<T, U, R> orElse(R orElse) {
		return (t, u) -> {
			try {
				return apply(t, u);
			} catch (Exception e) {
				return orElse;
			}
		};
	}

	default BiFunction<T, U, R> orElseGet(Supplier<? extends R> orElseGet) {
		requireNonNull(orElseGet);
		return (t, u) -> {
			try {
				return apply(t, u);
			} catch (Exception e) {
				return orElseGet.get();
			}
		};
	}

	static <T, U, R> BiFunction<T, U, R> asBiFunction(BiFunctionWithException<T, U, R> unchecked) {
		return unchecked.orElseThrow();
	}

	static <T, U, R> BiFunction<T, U, R> asBiFunctionOrElse(BiFunctionWithException<T, U, R> unchecked, R orElse) {
		return unchecked.orElse(orElse);
	}

	static <T, U, R> BiFunction<T, U, R> asBiFunctionOrElseGet(BiFunctionWithException<T, U, R> unchecked,
		Supplier<? extends R> orElseGet) {
		return unchecked.orElseGet(orElseGet);
	}
}
