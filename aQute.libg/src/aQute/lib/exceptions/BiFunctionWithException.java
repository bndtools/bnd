package aQute.lib.exceptions;

import java.util.function.BiFunction;

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

	static <T, U, R> BiFunction<T, U, R> asBiFunction(BiFunctionWithException<T, U, R> unchecked) {
		return (t, u) -> {
			try {
				return unchecked.apply(t, u);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
