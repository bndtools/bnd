package aQute.lib.exceptions;

import java.util.function.BiConsumer;

/**
 * Consumer interface that allows exceptions.
 * 
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 */
@FunctionalInterface
public interface BiConsumerWithException<T, U> {
	void accept(T t, U u) throws Exception;

	static <T, U> BiConsumer<T, U> asBiConsumer(BiConsumerWithException<T, U> unchecked) {
		return (t, u) -> {
			try {
				unchecked.accept(t, u);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
