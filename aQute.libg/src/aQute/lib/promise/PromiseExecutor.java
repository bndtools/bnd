package aQute.lib.promise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collector;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseExecutors;
import org.osgi.util.promise.Promises;

public class PromiseExecutor extends PromiseExecutors implements Executor {

	public PromiseExecutor() {
		this((runnable) -> runnable.run());
	}

	public PromiseExecutor(Executor executor) {
		super(Objects.requireNonNull(executor), null);
	}

	@Override
	public <V> Promise<V> submit(Callable< ? extends V> task) {
		return super.submit(task);
	}

	@Override
	public void execute(Runnable runnable) {
		executor().execute(runnable);
	}

	@Override
	public Executor executor() {
		return super.executor();
	}

	public <V, S extends V> Promise<List<V>> all(Collection<Promise<S>> promises) {
		return Promises.all(deferred(), promises);
	}

	public <V> Collector<Promise<V>,List<Promise<V>>,Promise<List<V>>> toAll() {
		return Collector.of(ArrayList::new, List::add, (l, r) -> {
			l.addAll(r);
			return l;
		}, this::all);
	}
}
