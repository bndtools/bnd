package aQute.lib.promise;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

public class PromiseExecutor implements Executor {
	private final Executor executor;

	public PromiseExecutor(Executor executor) {
		this.executor = Objects.requireNonNull(executor);
	}

	public <V> Promise<V> submit(final Callable< ? extends V> task) {
		final Deferred<V> deferred = new Deferred<>();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					deferred.resolve(task.call());
				} catch (Throwable t) {
					deferred.fail(t);
				}
			}
		};
		try {
			execute(runnable);
		} catch (Throwable t) {
			deferred.fail(t);
		}
		return deferred.getPromise();
	}

	@Override
	public void execute(Runnable runnable) {
		executor.execute(runnable);
	}
}
