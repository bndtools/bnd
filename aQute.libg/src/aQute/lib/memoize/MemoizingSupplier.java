package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

class MemoizingSupplier<T> implements Supplier<T> {
	private volatile boolean	delegate;
	// @GuardedBy("delegate")
	private Object				memoized;

	MemoizingSupplier(Supplier<? extends T> supplier) {
		memoized = requireNonNull(supplier);
		// write delegate _after_ write memoized
		delegate = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		// read delegate _before_ read memoized
		if (delegate) {
			// critical section: only one resolver at a time
			synchronized (this) {
				if (delegate) {
					T result = ((Supplier<? extends T>) memoized).get();
					memoized = result;
					// write delegate _after_ write memoized
					delegate = false;
					return result;
				}
			}
		}
		return (T) memoized;
	}
}
