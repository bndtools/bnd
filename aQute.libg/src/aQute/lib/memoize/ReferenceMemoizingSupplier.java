package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.function.Function;
import java.util.function.Supplier;

class ReferenceMemoizingSupplier<T> implements Supplier<T> {
	private final Supplier<? extends T>									supplier;
	private final Function<? super T, ? extends Reference<? extends T>>	reference;
	private volatile Reference<? extends T>								memoized;

	ReferenceMemoizingSupplier(Supplier<? extends T> supplier,
		Function<? super T, ? extends Reference<? extends T>> reference) {
		this.supplier = requireNonNull(supplier);
		this.reference = requireNonNull(reference);
		memoized = new WeakReference<>(null); // mark empty
	}

	@Override
	public T get() {
		T referent;
		if ((referent = memoized.get()) == null) {
			// critical section: only one at a time
			synchronized (this) {
				if ((referent = memoized.get()) == null) {
					referent = supplier.get();
					memoized = requireNonNull(reference.apply(referent));
				}
			}
		}
		return referent;
	}
}
