package aQute.lib.exceptions;

/**
 * BiFunction interface that allows exceptions *
 * 
 * @param <T1> the type 1 of the argument
 * @param <T2> the type 2 of the argument
 * @param <R> the result type
 */
public interface BiFunctionWithException<T1, T2, R> {
	R apply(T1 t, T2 t2) throws Throwable;
}
