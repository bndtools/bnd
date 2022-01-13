package aQute.bnd.stream;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class TakeWhile<T> extends AbstractWhile<T> {
	public static <O> Stream<O> takeWhile(Stream<O> stream, Predicate<? super O> predicate) {
		return StreamSupport.stream(new TakeWhile<>(stream.spliterator(), predicate), stream.isParallel())
			.onClose(stream::close);
	}

	private boolean take = true;

	private TakeWhile(Spliterator<T> spliterator, Predicate<? super T> predicate) {
		super(spliterator, predicate);
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		if (take) {
			while (spliterator.tryAdvance(this) && predicate.test(item)) {
				action.accept(item);
			}
			take = false;
		}
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (take) {
			if (spliterator.tryAdvance(this) && predicate.test(item)) {
				action.accept(item);
				return true;
			}
			take = false;
		}
		return false;
	}
}
