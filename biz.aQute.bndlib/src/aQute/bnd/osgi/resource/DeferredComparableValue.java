package aQute.bnd.osgi.resource;

import java.util.function.Supplier;

class DeferredComparableValue<T extends Comparable<T>> extends DeferredValue<T> implements Comparable<T> {

	DeferredComparableValue(Class<T> type, Supplier<? extends T> supplier, int hashCode) {
		super(type, supplier, hashCode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(T o) {
		if (o instanceof DeferredComparableValue) {
			o = ((DeferredComparableValue<T>) o).get();
		}
		return get().compareTo(o);
	}
}
