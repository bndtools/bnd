package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

class CloseableMemoizingSupplier<T extends AutoCloseable> implements CloseableMemoize<T> {
	private final Supplier<? extends T>	supplier;
	private volatile boolean			delegate;
	// @GuardedBy("delegate")
	private T							memoized;

	CloseableMemoizingSupplier(Supplier<? extends T> supplier) {
		this.supplier = requireNonNull(supplier);
		delegate = true;
	}

	@Override
	public T get() {
		// read delegate _before_ read memoized
		if (delegate) {
			// critical section: only one at a time
			synchronized (this) {
				if (delegate) {
					T result = supplier.get();
					memoized = result;
					// write delegate _after_ write memoized
					delegate = false;
					return result;
				}
			}
		}
		return memoized;
	}

	@Override
	public T peek() {
		// read delegate _before_ read memoized
		if (delegate) {
			return null;
		}
		return memoized;
	}

	@Override
	public void close() throws Exception {
		// read delegate _before_ read memoized
		if (delegate) {
			// critical section: only one at a time
			synchronized (this) {
				if (delegate) {
					delegate = false;
					return; // no value to close
				}
			}
		}
		T value = memoized;
		memoized = null; // release to GC
		// write delegate _after_ write memoized
		delegate = false; // even though it is already false
		if (value != null) {
			value.close();
		}
	}
}
