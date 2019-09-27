package aQute.lib.exceptions;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Function interface that allows exceptions.
 *
 * @param <T> the type of the argument
 * @param <R> the result type
 */
@FunctionalInterface
public interface FunctionWithException<T, R> {
	R apply(T t) throws Exception;

	default Function<T, R> orElseThrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default Function<T, R> orElse(R orElse) {
		return t -> {
			try {
				return apply(t);
			} catch (Exception e) {
				return orElse;
			}
		};
	}

	default Function<T, R> orElseGet(Supplier<? extends R> orElseGet) {
		requireNonNull(orElseGet);
		return t -> {
			try {
				return apply(t);
			} catch (Exception e) {
				return orElseGet.get();
			}
		};
	}

	static <T, R> Function<T, R> asFunction(FunctionWithException<T, R> unchecked) {
		return unchecked.orElseThrow();
	}

	static <T, R> Function<T, R> asFunctionOrElse(FunctionWithException<T, R> unchecked, R orElse) {
		return unchecked.orElse(orElse);
	}

	static <T, R> Function<T, R> asFunctionOrElseGet(FunctionWithException<T, R> unchecked,
		Supplier<? extends R> orElseGet) {
		return unchecked.orElseGet(orElseGet);
	}
}
