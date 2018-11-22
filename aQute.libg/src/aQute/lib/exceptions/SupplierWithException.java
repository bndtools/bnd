package aQute.lib.exceptions;

/**
 * Supplier interface that allows exceptions *
 * 
 * @param <R> the result type
 */
public interface SupplierWithException<R> {
	R get() throws Throwable;
}
