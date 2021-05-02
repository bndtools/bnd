package aQute.bnd.memoize;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The object can exist in one of two states:
 * <ul>
 * <li>expired which means that System.nanoTime is greater than timebound.
 * memoized may hold an expired value or null. The object transitions to this
 * state when time_to_live elapses. This is the initial state. From this state,
 * the object transitions to valued when @{code get} is called.</li>
 * <li>valued which means that System.nanoTime is less than timebound. memoized
 * holds the current value.</li>
 * </ul>
 */
class RefreshingMemoizingSupplier<T> implements Memoize<T> {
	private final Supplier<? extends T>	supplier;
	private final long					time_to_live;
	private volatile long				timebound;
	// @GuardedBy("timebound")
	private T							memoized;

	RefreshingMemoizingSupplier(Supplier<? extends T> supplier, long time_to_live, TimeUnit unit) {
		this.supplier = requireNonNull(supplier);
		this.time_to_live = requireNonNull(unit).toNanos((time_to_live < 0L) ? 0L : time_to_live);
		this.timebound = System.nanoTime(); // mark ttl expired
	}

	@Override
	public T get() {
		// read timebound _before_ read memoized
		long endtime = timebound;
		if (System.nanoTime() - endtime >= 0L) { // timebound has passed
			// critical section: only one at a time
			synchronized (this) {
				if (endtime == timebound) {
					T result = supplier.get();
					memoized = result;
					// write timebound _after_ write memoized
					timebound = System.nanoTime() + time_to_live;
					return result;
				}
			}
		}
		return memoized;
	}

	@Override
	public T peek() {
		// read timebound _before_ read memoized
		long endtime = timebound;
		if (System.nanoTime() - endtime >= 0L) { // timebound has passed
			// critical section: only one at a time
			synchronized (this) {
				if (endtime == timebound) {
					memoized = null; // release to GC
					// write timebound _after_ write memoized
					timebound = System.nanoTime(); // mark ttl expired
					return null;
				}
			}
		}
		return memoized;
	}
}
