package aQute.lib.exceptions;

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
}
