package aQute.lib.exceptions;

import static java.util.Objects.requireNonNull;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Predicate interface that allows exceptions.
 *
 * @param <T> the type of the argument
 */
@FunctionalInterface
public interface PredicateWithException<T> {
	boolean test(T t) throws Exception;

	default Predicate<T> orElseThrow() {
		return t -> {
			try {
				return test(t);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default Predicate<T> orElse(boolean orElse) {
		return t -> {
			try {
				return test(t);
			} catch (Exception e) {
				return orElse;
			}
		};
	}

	default Predicate<T> orElseGet(BooleanSupplier orElseGet) {
		requireNonNull(orElseGet);
		return t -> {
			try {
				return test(t);
			} catch (Exception e) {
				return orElseGet.getAsBoolean();
			}
		};
	}

	static <T> Predicate<T> asPredicate(PredicateWithException<T> unchecked) {
		return unchecked.orElseThrow();
	}

	static <T> Predicate<T> asPredicateOrElse(PredicateWithException<T> unchecked, boolean orElse) {
		return unchecked.orElse(orElse);
	}

	static <T> Predicate<T> asPredicateOrElseGet(PredicateWithException<T> unchecked, BooleanSupplier orElseGet) {
		return unchecked.orElseGet(orElseGet);
	}
}
