package aQute.lib.exceptions;

import static java.util.Objects.requireNonNull;

import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;

/**
 * BiPredicate interface that allows exceptions.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 */
@FunctionalInterface
public interface BiPredicateWithException<T, U> {
	boolean test(T t, U u) throws Exception;

	default BiPredicate<T, U> orElseThrow() {
		return (t, u) -> {
			try {
				return test(t, u);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default BiPredicate<T, U> orElse(boolean orElse) {
		return (t, u) -> {
			try {
				return test(t, u);
			} catch (Exception e) {
				return orElse;
			}
		};
	}

	default BiPredicate<T, U> orElseGet(BooleanSupplier orElseGet) {
		requireNonNull(orElseGet);
		return (t, u) -> {
			try {
				return test(t, u);
			} catch (Exception e) {
				return orElseGet.getAsBoolean();
			}
		};
	}

	static <T, U> BiPredicate<T, U> asBiPredicate(BiPredicateWithException<T, U> unchecked) {
		return unchecked.orElseThrow();
	}

	static <T, U> BiPredicate<T, U> asBiPredicateOrElse(BiPredicateWithException<T, U> unchecked, boolean orElse) {
		return unchecked.orElse(orElse);
	}

	static <T, U> BiPredicate<T, U> asBiPredicateOrElseGet(BiPredicateWithException<T, U> unchecked,
		BooleanSupplier orElseGet) {
		return unchecked.orElseGet(orElseGet);
	}
}
