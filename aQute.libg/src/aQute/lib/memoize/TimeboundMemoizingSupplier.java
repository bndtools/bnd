package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class TimeboundMemoizingSupplier<T> implements Supplier<T> {
	private final Supplier<? extends T>	supplier;
	private final long					duration;
	private volatile long				timebound;
	// @GuardedBy("timebound")
	private T							memoized;

	TimeboundMemoizingSupplier(Supplier<? extends T> supplier, long duration, TimeUnit unit) {
		this.supplier = requireNonNull(supplier);
		this.duration = requireNonNull(unit).toNanos((duration < 0L) ? 0L : duration);
		this.timebound = System.nanoTime(); // mark expired
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
					timebound = System.nanoTime() + duration;
					return result;
				}
			}
		}
		return memoized;
	}
}
