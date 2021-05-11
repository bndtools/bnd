package aQute.lib.concurrent.serial;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

/**
 * Will execute a set of tasks in order of submit.
 */
public class SerialExecutor implements Closeable {
	final Executor			executor;
	final List<Runnable>	tasks	= new ArrayList<>();
	volatile Thread			thread;

	/**
	 * The executor to use.
	 *
	 * @param executor
	 */
	public SerialExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Run serial in order of submission and return a promise
	 *
	 * @param <T> the return type
	 * @param callable the callable providing the data
	 * @return the promise
	 */
	public <T> Promise<T> submit(Callable<T> callable) {
		Deferred<T> deferred = new Deferred<>();
		Runnable r = () -> {
			try {
				T value = callable.call();
				deferred.resolve(value);
			} catch (Exception e) {
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
			tasks.add(runnable);
			if (tasks.size() == 1) {
				executor.execute(() -> {
					thread = Thread.currentThread();
					while (true) {
						Runnable r;
						synchronized (tasks) {
							if (tasks.isEmpty()) {
								thread = null;
								return;
							}

							r = tasks.remove(0);
						}
						r.run();
					}
				});
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (tasks) {
			tasks.clear();
			Thread t = thread;
			if (t != null) {
				t.interrupt();
			}
		}

	}
}
