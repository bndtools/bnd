package aQute.lib.memoize;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class Memoize {

	private Memoize() {}

	public static <T> Supplier<T> supplier(Supplier<? extends T> delegate) {
		if (delegate instanceof MemoizingSupplier) {
			@SuppressWarnings("unchecked")
			Supplier<T> supplier = (Supplier<T>) delegate;
			return supplier;
		}
		return new MemoizingSupplier<>(delegate);
	}

	public static <T, R> Supplier<R> supplier(Function<? super T, ? extends R> function, T argument) {
		requireNonNull(function);
		return supplier(() -> function.apply(argument));
	}
}
