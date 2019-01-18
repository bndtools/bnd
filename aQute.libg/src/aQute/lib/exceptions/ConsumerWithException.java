package aQute.lib.exceptions;

import java.util.function.Consumer;

/**
 * Consumer interface that allows exceptions.
 * 
 * @param <T> the type of the argument
 */
@FunctionalInterface
public interface ConsumerWithException<T> {
	void accept(T t) throws Exception;

	static <T> Consumer<T> asConsumer(ConsumerWithException<T> unchecked) {
		return t -> {
			try {
				unchecked.accept(t);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
