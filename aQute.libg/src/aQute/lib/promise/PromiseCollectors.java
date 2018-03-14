package aQute.lib.promise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

public class PromiseCollectors {

	private PromiseCollectors() {}

	public static <V> Collector<Promise<V>, List<Promise<V>>, Promise<List<V>>> toPromise(
		PromiseFactory promiseFactory) {
		return Collector.of(ArrayList::new, List::add, PromiseCollectors::combiner, promiseFactory::all);
	}

	private static <E, C extends Collection<E>> C combiner(C t, C u) {
		t.addAll(u);
		return t;
	}
}
