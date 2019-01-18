package aQute.lib.exceptions;

/**
 * Supplier interface that allows exceptions.
 * 
 * @param <R> the result type
 */
@FunctionalInterface
public interface SupplierWithException<R> {
	R get() throws Exception;
}
