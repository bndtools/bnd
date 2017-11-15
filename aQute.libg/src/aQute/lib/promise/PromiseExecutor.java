package aQute.lib.promise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collector;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

public class PromiseExecutor extends PromiseFactory implements Executor {

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

	public <V> Collector<Promise<V>,List<Promise<V>>,Promise<List<V>>> toPromise() {
		return Collector.of(ArrayList::new, List::add, PromiseExecutor::combiner, this::all);
	}

	private static <E, C extends Collection<E>> C combiner(C t, C u) {
		t.addAll(u);
		return t;
	}
}
