package aQute.bnd.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

/**
 * The object can exist in one of two states:
 * <ul>
 * <li>initial which means {@code get} has not been called and memoized holds
 * the wrapped supplier. From this state, the object can transition to
 * open.</li>
 * <li>open which means memoized is the value from the wrapped supplier. This is
 * the terminal state.</li>
 * </ul>
 */
class MemoizingSupplier<T> implements Memoize<T> {
	private volatile boolean	initial;
	// @GuardedBy("initial")
	private Object				memoized;

	MemoizingSupplier(Supplier<? extends T> supplier) {
		memoized = requireNonNull(supplier);
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
					T result = ((Supplier<? extends T>) memoized).get();
					memoized = result;
					// write initial _after_ write memoized
					initial = false;
					return result;
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
}
