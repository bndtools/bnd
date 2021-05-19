package aQute.bnd.memoize;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
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
	private final StampedLock			lock;
	private volatile boolean			initial;
	// @GuardedBy("initial")
	private Object						memoized;

	CloseableMemoizingSupplier(Supplier<? extends T> supplier) {
		requireNonNull(supplier);
		memoized = (Supplier<T>) () -> {
			T result = supplier.get();
			memoized = result;
			// write initial _after_ write memoized
			initial = false;
			return result;
		};
		initial = true;
		lock = new StampedLock();
	}

	@Override
	public T get() {
		// read initial _before_ read memoized
		if (initial) {
			// critical section: only one at a time
			final long stamp = lock.writeLock();
			try {
				return initial();
			} finally {
				lock.unlockWrite(stamp);
			}
		}
		return value(memoized);
	}

	// @GuardedBy("lock.writeLock()")
	@SuppressWarnings("unchecked")
	private T initial() {
		if (initial) {
			T result = ((Supplier<T>) memoized).get();
			return value(result);
		}
		return value(memoized);
	}

	@SuppressWarnings("unchecked")
	private static <T extends AutoCloseable> T value(Object value) {
		if (value == null) {
			throw new IllegalStateException("closed");
		}
		return (T) value;
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
		// read initial _before_ read memoized
		return !initial && (memoized != null);
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
				// read initial _before_ read memoized
				if (initial) {
					memoized = null; // mark closed
					// write initial _after_ write memoized
					initial = false;
					return; // no value to close
				}
				closeable = (AutoCloseable) memoized;
				if (closeable == null) {
					return; // already closed
				}
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
		requireNonNull(consumer);
		// prevent closing during accept while allowing multiple accepts
		if (initial) {
			// critical section: only one at a time
			long stamp = lock.tryWriteLock();
			if (stamp != 0L) {
				try {
					T value = initial();
					// downgrade to readLock before calling consumer
					stamp = lock.tryConvertToReadLock(stamp);
					consumer.accept(value);
				} finally {
					lock.unlock(stamp);
				}
				return this;
			}
		}
		final long stamp = lock.readLock();
		try {
			T value = value(memoized);
			consumer.accept(value);
		} finally {
			lock.unlockRead(stamp);
		}
		return this;
	}

	@Override
	public CloseableMemoize<T> ifPresent(Consumer<? super T> consumer) {
		// read initial _before_ read memoized
		if (isPresent()) {
			requireNonNull(consumer);
			// prevent closing during accept while allowing multiple accepts
			final long stamp = lock.readLock();
			try {
				@SuppressWarnings("unchecked")
				T value = (T) memoized;
				if (value != null) { // may have been just closed
					consumer.accept(value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
		return this;
	}

	@Override
	public String toString() {
		// read initial _before_ read memoized
		return initial ? "<empty>" : Objects.toString(memoized, "<closed>");
	}
}
