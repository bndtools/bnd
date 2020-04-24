package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

class CloseableMemoizingSupplier<T extends AutoCloseable> implements CloseableMemoize<T> {
	private final Supplier<? extends T>	supplier;
	// @GuardedBy("this")
	private boolean						closed	= false;
	private volatile T					memoized;

	CloseableMemoizingSupplier(Supplier<? extends T> supplier) {
		this.supplier = requireNonNull(supplier);
	}

	@Override
	public T get() {
		if (memoized == null) {
			synchronized (this) {
				if (closed)
					throw new IllegalStateException("Already closed");
				return get0();
			}
		}
		return memoized;
	}

	private T get0() {
		if (memoized == null) {
			memoized = supplier.get();
			assert memoized != null;
		}
		return memoized;
	}

	@Override
	public T peek() {
		return memoized;
	}

	@Override
	public synchronized boolean isClosed() {
		return closed;
	}

	@Override
	public void close() throws Exception {
		T current;
		synchronized (this) {
			if (closed)
				return;

			closed = true;

			if (memoized == null)
				return;

			current = memoized;
			memoized = null;
		}
		current.close();
	}

	@Override
	public Memoize<T> accept(Consumer<? super T> consumer) {
		synchronized (this) {
			if (closed) {
				throw new IllegalStateException("Already closed");
			}
			consumer.accept(get0());
		}
		return this;
	}
}
