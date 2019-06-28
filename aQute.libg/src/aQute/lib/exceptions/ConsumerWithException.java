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

	default Consumer<T> orElseThrow() {
		return t -> {
			try {
				accept(t);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default Consumer<T> ignoreException() {
		return t -> {
			try {
				accept(t);
			} catch (Exception e) {}
		};
	}

	static <T> Consumer<T> asConsumer(ConsumerWithException<T> unchecked) {
		return unchecked.orElseThrow();
	}

	static <T> Consumer<T> asConsumerIgnoreException(ConsumerWithException<T> unchecked) {
		return unchecked.ignoreException();
	}
}
