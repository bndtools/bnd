package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

class MemoizingSupplier<T> implements Supplier<T> {
	private volatile boolean		empty;
	// @GuardedBy("empty")
	private Supplier<? extends T>	delegate;
	// @GuardedBy("empty")
	private T						memoized;

	MemoizingSupplier(Supplier<? extends T> delegate) {
		this.delegate = requireNonNull(delegate);
		// write empty _after_ write delegate
		this.empty = true;
	}

	@Override
	public T get() {
		// read empty _before_ read memoized
		if (empty) {
			// critical section: only one resolver at a time
			synchronized (this) {
				if (empty) {
					T result = delegate.get();
					memoized = result;
					delegate = null; // dereference for GC
					// write empty _after_ write memoized
					empty = false;
					return result;
				}
			}
		}
		return memoized;
	}
}
