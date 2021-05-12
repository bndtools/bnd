package aQute.lib.concurrent.serial;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will execute a set of tasks in order of submit.
 */
public class SerialExecutor implements AutoCloseable {
	final static Logger		logger	= LoggerFactory.getLogger(SerialExecutor.class);

	final Executor			executor;
	final Deque<Runnable>	tasks	= new ArrayDeque<>();
	final PromiseFactory	factory;
	volatile Thread			thread;

	/**
	 * The executor to use.
	 *
	 * @param executor
	 */
	public SerialExecutor(Executor executor) {
		this.executor = executor;
		this.factory = new PromiseFactory(executor);
	}

	/**
	 * Run serial in order of submission and return a promise
	 *
	 * @param <T> the return type
	 * @param callable the callable providing the data
	 * @return the promise
	 */
	public <T> Promise<T> submit(Callable<T> callable) {
		Deferred<T> deferred = factory.deferred();
		Runnable r = () -> {
			try {
				T value = callable.call();
				deferred.resolve(value);
			} catch (Throwable e) {
				deferred.fail(e);
			}
		};
		run(r);
		return deferred.getPromise();
	}

	/**
	 * Run the runnable in order of submission.
	 *
	 * @param runnable the runnable
	 */
	public void run(Runnable runnable) {
		synchronized (tasks) {
			tasks.push(runnable);
			if (tasks.size() == 1) {
				executor.execute(() -> {
					thread = Thread.currentThread();
					while (true) {
						Runnable r;
						synchronized (tasks) {
							if (tasks.isEmpty() || thread.isInterrupted()) {
								thread = null;
								return;
							}

							r = tasks.pop();
						}

						try {
							r.run();
						} catch (Throwable e) {
							logger.warn("failed to execute task {} {}", runnable, e, e);
						}
					}
				});
			}
		}
	}

	@Override
	public void close() {
		synchronized (tasks) {
			tasks.clear();
			Thread t = thread;
			if (t != null) {
				t.interrupt();
			}
		}

	}
}
