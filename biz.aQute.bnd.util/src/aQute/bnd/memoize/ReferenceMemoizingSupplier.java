package aQute.bnd.memoize;

import static java.util.Objects.requireNonNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The object can exist in one of two states:
 * <ul>
 * <li>cleared which means memoized holds a cleared reference. This is the
 * initial state. The object transitions to this state if the garbage collector
 * clears the reference. From this state, the object transitions to valued
 * when @{code get} is called.</li>
 * <li>valued which means memoized holds a reference with a value.</li>
 * </ul>
 */
class ReferenceMemoizingSupplier<T> implements Memoize<T> {
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

	@Override
	public T peek() {
		T referent = memoized.get();
		return referent;
	}
}
