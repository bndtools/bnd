package aQute.lib.exceptions;

import java.util.function.Predicate;

/**
 * Predicate interface that allows exceptions.
 * 
 * @param <T> the type of the argument
 */
@FunctionalInterface
public interface PredicateWithException<T> {
	boolean test(T t) throws Exception;

	static <T> Predicate<T> asPredicate(PredicateWithException<T> unchecked) {
		return t -> {
			try {
				return unchecked.test(t);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
