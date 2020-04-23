package aQute.lib.lazy;

import java.util.function.Supplier;

import aQute.lib.exceptions.ConsumerWithException;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.exceptions.SupplierWithException;

/**
 * A constructor like function. The Lazy will initialize at first use. It also
 * provides 'safe' functions to use the lazy object and postponing the close
 * until at end. If this object is closed, it can be reopened. Closing will also
 * close the target if it implements AutoCloseable.
 *
 * @param <T> the type
 */
public class Lazy<T> implements AutoCloseable, Supplier<T> {
	final SupplierWithException<T>	supplier;
	T								target;

	/**
	 * Factory function
	 *
	 * @param supplier factory
	 */
	public Lazy(SupplierWithException<T> supplier) {
		assert supplier != null;
		this.supplier = supplier;
	}

	/**
	 * Close this object, this will reset the target. It will be reopened when
	 * get is called again.
	 */
	@Override
	public synchronized void close() {

		if (target instanceof AutoCloseable)
			try {
				((AutoCloseable) target).close();
			} catch (Exception e) {
				// ignore
			}
		this.target = null;
	}

	/**
	 * Double locking does work on the Java 5+ memory model, see at end
	 * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
	 * <p>
	 * It allows the majority of cases to only cross a read barrier
	 */
	@Override
	public synchronized T get() {
		return get0();
	}

	/**
	 * Run a consumer with the target, delaying close until finished. I.e. this
	 * function shouldn't stay away forever.
	 *
	 * @param c the consumer
	 */
	public synchronized void apply(ConsumerWithException<T> c) {
		try {
			c.accept(get0());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Run a function with the target, delaying close until finished. I.e. this
	 * function shouldn't stay away forever.
	 *
	 * @param c the function
	 */
	public synchronized <R> R map(FunctionWithException<T, R> c) {
		try {
			return c.apply(get0());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Check if initialized. Clearly this is a snapshot
	 *
	 * @return true if initialized
	 */
	public synchronized boolean isInitialized() {
		return target != null;
	}

	/**
	 * Check if initialized. Clearly this is a snapshot
	 *
	 * @return true if initialized
	 */
	public synchronized T peek() {
		return target;
	}

	private T get0() {
		if (target == null)
			try {
				target = supplier.get();
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		return target;
	}

}
