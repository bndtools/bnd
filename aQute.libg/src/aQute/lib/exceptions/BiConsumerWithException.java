package aQute.lib.exceptions;

/**
 * Consumer interface that allows exceptions.
 * 
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 */
@FunctionalInterface
public interface BiConsumerWithException<T, U> {
	void accept(T t, U u) throws Exception;
}
