package aQute.bnd.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The object can exist in one of two states:
 * <ul>
 * <li>initial which means the source supplier has not been called or has not
 * returned a value acceptable to the predicate and memoized holds the wrapped
 * supplier. From this state, the object can transition to open.</li>
 * <li>open which means memoized is the value from the source supplier. This is
 * the terminal state.</li>
 * </ul>
 */
class MemoizingSupplier<T> implements Memoize<T> {
	private volatile boolean	initial;
	// @GuardedBy("initial")
	private Object				memoized;

	MemoizingSupplier(Supplier<? extends T> supplier) {
		this(supplier, supplied -> true);
	}

	MemoizingSupplier(Supplier<? extends T> supplier, Predicate<? super T> predicate) {
		requireNonNull(supplier);
		requireNonNull(predicate);
		memoized = (Supplier<T>) () -> {
			T supplied = supplier.get();
			if (predicate.test(supplied)) {
				memoized = supplied;
				// write initial _after_ write memoized
				initial = false;
			}
			return supplied;
		};
		// write initial _after_ write memoized
		initial = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		// read initial _before_ read memoized
		if (initial) {
			// critical section: only one resolver at a time
			synchronized (this) {
				if (initial) {
					return ((Supplier<T>) memoized).get();
				}
			}
		}
		return (T) memoized;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T peek() {
		// read initial _before_ read memoized
		if (initial) {
			return null;
		}
		return (T) memoized;
	}

	@Override
	public boolean isPresent() {
		return !initial;
	}

	@Override
	public String toString() {
		// read initial _before_ read memoized
		return initial ? "<empty>" : String.valueOf(memoized);
	}
}
