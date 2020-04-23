package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
