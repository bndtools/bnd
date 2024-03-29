package aQute.bnd.osgi.resource;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

import aQute.bnd.memoize.Memoize;

class DeferredValue<T> implements Supplier<T> {
	private final Class<T>				type;
	private final Supplier<? extends T>	supplier;
	private final int					hashCode;

	DeferredValue(Class<T> type, Supplier<? extends T> supplier, int hashCode) {
		this.type = requireNonNull(type);
		this.supplier = Memoize.supplier(supplier);
		this.hashCode = hashCode;
	}

	@Override
	public T get() {
		return supplier.get();
	}

	Class<T> type() {
		return type;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DeferredValue) {
			obj = ((DeferredValue<T>) obj).get();
		}
		return get().equals(obj);
	}

	@Override
	public String toString() {
		return String.valueOf(get());
	}
}
