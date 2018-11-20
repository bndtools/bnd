package aQute.lib.exceptions;

/**
 * Consumer interface that allows exceptions *
 * 
 * @param <T> the type of the argument
 */
public interface ConsumerWithException<T> {
	void apply(T t) throws Throwable;
}
