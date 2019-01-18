package aQute.lib.exceptions;

/**
 * Consumer interface that allows exceptions.
 * 
 * @param <T> the type of the argument
 */
@FunctionalInterface
public interface ConsumerWithException<T> {
	void accept(T t) throws Exception;
}
