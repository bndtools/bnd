package aQute.bnd.service.result;

/**
 * Function interface that allows exceptions.
 *
 * @param <T> the type of the argument
 * @param <R> the result type
 */
@FunctionalInterface
public interface FunctionWithException<T, R> {
	R apply(T t) throws Exception;
}
