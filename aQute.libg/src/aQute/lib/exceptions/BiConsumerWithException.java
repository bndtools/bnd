package aQute.lib.exceptions;

/**
 * Consumer interface that allows exceptions *
 * 
 * @param <T1> the type of the first argument
 * @param <T2> the type of the second argument
 */
public interface BiConsumerWithException<T1, T2> {
	void apply(T1 t1, T2 t2) throws Throwable;
}
