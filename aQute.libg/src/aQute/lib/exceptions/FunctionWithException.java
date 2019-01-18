package aQute.lib.exceptions;

import java.util.function.Function;

/**
 * Function interface that allows exceptions.
 * 
 * @param <T> the type of the argument
 * @param <R> the result type
 */
@FunctionalInterface
public interface FunctionWithException<T, R> {
	R apply(T t) throws Exception;

	static <T, R> Function<T, R> asFunction(FunctionWithException<T, R> unchecked) {
		return t -> {
			try {
				return unchecked.apply(t);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
