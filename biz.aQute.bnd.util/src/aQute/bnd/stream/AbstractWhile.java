package aQute.bnd.stream;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

abstract class AbstractWhile<T> implements Spliterator<T>, Consumer<T> {
	final Spliterator<T>		spliterator;
	final Predicate<? super T>	predicate;
	T							item;

	AbstractWhile(Spliterator<T> spliterator, Predicate<? super T> predicate) {
		this.spliterator = requireNonNull(spliterator);
		this.predicate = requireNonNull(predicate);
	}

	@Override
	public Spliterator<T> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return spliterator.estimateSize();
	}

	@Override
	public long getExactSizeIfKnown() {
		return -1L;
	}

	@Override
	public int characteristics() {
		return spliterator.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
	}

	@Override
	public Comparator<? super T> getComparator() {
		return spliterator.getComparator();
	}

	@Override
	public void accept(T item) {
		this.item = item;
	}
}
