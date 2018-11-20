package aQute.lib.exceptions;

/**
 * Function interface that allows exceptions *
 * 
 * @param <T> the type of the argument
 * @param <R> the result type
 */
public interface FunctionWithException<T, R> {
	R apply(T t) throws Throwable;
}
