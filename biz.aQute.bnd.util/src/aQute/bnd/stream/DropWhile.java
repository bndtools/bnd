package aQute.bnd.stream;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class DropWhile<T> extends AbstractWhile<T> {
	public static <O> Stream<O> dropWhile(Stream<O> stream, Predicate<? super O> predicate) {
		return StreamSupport.stream(new DropWhile<>(stream.spliterator(), predicate), stream.isParallel())
			.onClose(stream::close);
	}

	private boolean drop = true;

	private DropWhile(Spliterator<T> spliterator, Predicate<? super T> predicate) {
		super(spliterator, predicate);
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		if (drop) {
			drop = false;
			while (spliterator.tryAdvance(this)) {
				if (!predicate.test(item)) {
					action.accept(item);
					break;
				}
			}
		}
		spliterator.forEachRemaining(action);
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (drop) {
			drop = false;
			while (spliterator.tryAdvance(this)) {
				if (!predicate.test(item)) {
					action.accept(item);
					return true;
				}
			}
			return false;
		}
		return spliterator.tryAdvance(action);
	}
}
