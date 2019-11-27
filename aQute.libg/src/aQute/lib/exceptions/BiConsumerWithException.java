package aQute.lib.exceptions;

import java.util.function.BiConsumer;

/**
 * BiConsumer interface that allows exceptions.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 */
@FunctionalInterface
public interface BiConsumerWithException<T, U> {
	void accept(T t, U u) throws Exception;

	default BiConsumer<T, U> orElseThrow() {
		return (t, u) -> {
			try {
				accept(t, u);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default BiConsumer<T, U> ignoreException() {
		return (t, u) -> {
			try {
				accept(t, u);
			} catch (Exception e) {}
		};
	}

	static <T, U> BiConsumer<T, U> asBiConsumer(BiConsumerWithException<T, U> unchecked) {
		return unchecked.orElseThrow();
	}

	static <T, U> BiConsumer<T, U> asBiConsumerIgnoreException(BiConsumerWithException<T, U> unchecked) {
		return unchecked.ignoreException();
	}
}
