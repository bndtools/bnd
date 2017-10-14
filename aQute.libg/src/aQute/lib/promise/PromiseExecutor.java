package aQute.lib.promise;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

public class PromiseExecutor implements Executor {
	private final Executor executor;

	public PromiseExecutor() {
		this.executor = (runnable) -> runnable.run();
	}

	public PromiseExecutor(Executor executor) {
		this.executor = Objects.requireNonNull(executor);
	}

	public <V> Promise<V> submit(Callable< ? extends V> task) {
		Objects.requireNonNull(task);
		Deferred<V> deferred = deferred();
		try {
			execute(() -> {
				try {
					deferred.resolve(task.call());
				} catch (Throwable t) {
					deferred.fail(t);
				}
			});
		} catch (Throwable t) {
			deferred.fail(t);
		}
		return deferred.getPromise();
	}

	@Override
	public void execute(Runnable runnable) {
		executor.execute(runnable);
	}

	public <V> Deferred<V> deferred() {
		return new Deferred<>();
	}

	public <V> Promise<V> resolved(V value) {
		Deferred<V> deferred = deferred();
		deferred.resolve(value);
		return deferred.getPromise();
	}

	public <V> Promise<V> failed(Throwable failure) {
		Deferred<V> deferred = deferred();
		deferred.fail(failure);
		return deferred.getPromise();
	}

	public <V, S extends V> Promise<List<V>> all(Collection<Promise<S>> promises) {
		return Promises.all(promises);
	}
}
