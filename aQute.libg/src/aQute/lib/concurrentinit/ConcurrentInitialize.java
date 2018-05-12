package aQute.lib.concurrentinit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to handle concurrent system where you need to initialize a
 * value. The first one should create the value but the others should block
 * until the value has been created.
 *
 * @param <T>
 */
public abstract class ConcurrentInitialize<T> {
	private final AtomicBoolean		initializer	= new AtomicBoolean(false);
	private final CountDownLatch	resolved	= new CountDownLatch(1);
	private Thread					creator;
	private T						value;
	private Exception				exception;

	/**
	 * Get the value or wait until it is created.
	 */
	public T get() throws Exception {
		if (initializer.compareAndSet(false, true)) {
			creator = Thread.currentThread();
			try {
				value = create();
			} catch (Exception e) {
				exception = e;
			} finally {
				creator = null;
				resolved.countDown();
			}
		} else {
			Thread t = creator;
			if ((t != null) && (t == Thread.currentThread())) {
				throw new IllegalStateException("Cycle: ConcurrentInitialize's create returns to same instance");
			}
			resolved.await();
		}

		if (exception == null) {
			return value;
		}
		throw exception;
	}

	/**
	 * Override to create the actual object
	 *
	 * @return the actual object, could be null
	 * @throws Exception if the creation failed this is the exception that was
	 *             thrown
	 */
	public abstract T create() throws Exception;
}
