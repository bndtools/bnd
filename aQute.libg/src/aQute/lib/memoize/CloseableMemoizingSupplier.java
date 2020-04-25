package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The object can exist in one of three states:
 * <ul>
 * <li>initial which means neither {@code get} or {@code close} has been called.
 * From this state, the object can transition directly to either open or
 * closed.</li>
 * <li>open which means memoized is non-null with the value from the wrapped
 * supplier. From this state, the object can transition to closed.</li>
 * <li>closed which means memoized is null and any value it may have held was
 * closed. This is a terminal state.</li>
 * </ul>
 */
class CloseableMemoizingSupplier<T extends AutoCloseable> implements CloseableMemoize<T> {
	private final Supplier<? extends T>	supplier;
	private final StampedLock			lock;
	private volatile boolean			initial;
	// @GuardedBy("initial")
	private T							memoized;

	CloseableMemoizingSupplier(Supplier<? extends T> supplier) {
		this.supplier = requireNonNull(supplier);
		lock = new StampedLock();
		initial = true;
	}

	@Override
	public T get() {
		// read initial _before_ read memoized
		if (initial) {
			// critical section: only one at a time
			final long stamp = lock.writeLock();
			try {
				if (initial) {
					T result = supplier.get();
					memoized = result;
					// write initial _after_ write memoized
					initial = false;
					if (result == null) {
						throw new IllegalStateException("closed");
					}
					return result;
				}
			} finally {
				lock.unlockWrite(stamp);
			}
		}
		T result = memoized;
		if (result == null) {
			throw new IllegalStateException("closed");
		}
		return result;
	}

	@Override
	public T peek() {
		// read initial _before_ read memoized
		if (initial) {
			return null;
		}
		return memoized;
	}

	@Override
	public boolean isClosed() {
		// read initial _before_ read memoized
		return !initial && (memoized == null);
	}

	@Override
	public void close() throws Exception {
		if (!isClosed()) {
			AutoCloseable closeable;
			// critical section: only one at a time
			final long stamp = lock.writeLock();
			try {
				if (initial) {
					initial = false;
					return; // no value to close
				}
				closeable = memoized;
				memoized = null; // mark closed
				// write initial _after_ write memoized
				initial = false; // even though it is already false
			} finally {
				lock.unlockWrite(stamp);
			}
			closeable.close(); // it is safe to let any exception propagate
		}
	}

	@Override
	public CloseableMemoize<T> accept(Consumer<? super T> consumer) {
		T value = get(); // may need write lock
		// prevent closing during accept while allowing multiple accepts
		final long stamp = lock.readLock();
		try {
			if (isClosed()) {
				throw new IllegalStateException("closed");
			}
			consumer.accept(value);
		} finally {
			lock.unlockRead(stamp);
		}
		return this;
	}
}
