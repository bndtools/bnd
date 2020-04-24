package aQute.lib.memoize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.Closeable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemoizeTest {
	AtomicInteger		count;
	Supplier<String>	source;

	@BeforeEach
	public void setup() {
		count = new AtomicInteger();
		source = () -> Integer.toString(count.incrementAndGet());
	}

	@Test
	public void supplier() {
		Memoize<String> memoized = Memoize.supplier(source);
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("1");
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isEqualTo("1");
		assertThat(count).hasValue(1);
	}

	@Test
	public void supplier_null() {
		assertThatNullPointerException().isThrownBy(() -> Memoize.supplier(null));
	}

	@Test
	public void supplier_function() {
		Function<AtomicInteger, String> function = count -> Integer.toString(count.incrementAndGet());
		Memoize<String> memoized = Memoize.supplier(function, count);
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("1");
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isEqualTo("1");
		assertThat(count).hasValue(1);
	}

	@Test
	public void supplier_function_null() {
		assertThatNullPointerException()
			.isThrownBy(() -> Memoize.supplier((Function<Object, Object>) null, new Object()));
	}

	@Test
	public void refreshing() throws Exception {
		Memoize<String> memoized = Memoize.refreshingSupplier(source, 100, TimeUnit.MILLISECONDS);
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("1");
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isEqualTo("1");
		assertThat(count).hasValue(1);

		sleep(200, TimeUnit.MILLISECONDS);

		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("2");
		assertThat(count).hasValue(2);
		assertThat(memoized.peek()).isEqualTo("2");
		assertThat(count).hasValue(2);

		sleep(200, TimeUnit.MILLISECONDS);

		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(2);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("3");
		assertThat(count).hasValue(3);
		assertThat(memoized.peek()).isEqualTo("3");
		assertThat(count).hasValue(3);
	}

	void sleep(long duration, TimeUnit unit) throws InterruptedException {
		for (long end = System.nanoTime() + unit.toNanos(duration), delay; (delay = end - System.nanoTime()) >= 0L;) {
			Thread.sleep(TimeUnit.NANOSECONDS.toMillis(delay));
		}
	}

	@Test
	public void refreshing_null() {
		assertThatNullPointerException()
			.isThrownBy(() -> Memoize.refreshingSupplier((Supplier<Object>) null, 100, TimeUnit.MILLISECONDS));
		assertThatNullPointerException().isThrownBy(() -> Memoize.refreshingSupplier(() -> "", 100, null));
	}

	@Test
	public void reference() throws Exception {
		ReferenceQueue<String> queue = new ReferenceQueue<>();
		Memoize<String> memoized = Memoize.referenceSupplier(source, t -> new WeakReference<>(t, queue));
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("1");
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isEqualTo("1");
		assertThat(count).hasValue(1);

		System.gc();
		assertThat(queue.remove(1000)).isNotNull();
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("2");
		assertThat(count).hasValue(2);
		assertThat(memoized.peek()).isEqualTo("2");
		assertThat(count).hasValue(2);

		System.gc();
		assertThat(queue.remove(1000)).isNotNull();
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(2);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("3");
		assertThat(count).hasValue(3);
		assertThat(memoized.peek()).isEqualTo("3");
		assertThat(count).hasValue(3);
	}

	@Test
	public void reference_null() {
		assertThatNullPointerException()
			.isThrownBy(() -> Memoize.referenceSupplier((Supplier<Object>) null, WeakReference::new));
		assertThatNullPointerException().isThrownBy(() -> Memoize.referenceSupplier(() -> "", null));
		assertThatNullPointerException().isThrownBy(() -> Memoize.referenceSupplier(() -> "", t -> null)
			.get());
	}

	@Test
	public void accept() {
		Memoize<String> memoized = Memoize.supplier(source);
		AtomicReference<Object> value = new AtomicReference<>();
		memoized.accept(value::set);
		assertThat(value).hasValue("1");
	}

	@Test
	public void filter() {
		Memoize<String> memoized = Memoize.supplier(source);
		Memoize<String> filtered = memoized.filter("1"::equals);
		assertThat(Stream.generate(filtered::get)
			.limit(10)).containsOnly("1");

		filtered = memoized.filter("2"::equals);
		assertThat(filtered.get()).isNull();
		assertThat(filtered.peek()).isNull();
	}

	@Test
	public void map() {
		Memoize<String> memoized = Memoize.supplier(source);
		Memoize<Number> mapped = memoized.map(Integer::decode);
		assertThat(Stream.generate(mapped::get)
			.limit(10)).containsOnly(Integer.valueOf(1));
	}

	@Test
	public void flatMap() {
		Memoize<String> memoized = Memoize.supplier(source);
		Supplier<Integer> supplier = () -> Integer.valueOf(10);
		Memoize<Number> mapped = memoized.flatMap(s -> supplier);
		assertThat(Stream.generate(mapped::get)
			.limit(10)).containsOnly(Integer.valueOf(10));
	}

	static class CloseableClass implements Closeable {
		boolean		closed	= false;
		final int	count;

		CloseableClass(int count) {
			this.count = count;
		}

		@Override
		public void close() {
			closed = true;
		}

		@Override
		public String toString() {
			return Integer.toString(count);
		}
	}

	@Test
	public void closeable() throws Exception {
		Supplier<CloseableClass> source = () -> new CloseableClass(count.incrementAndGet());
		CloseableMemoize<CloseableClass> memoized = CloseableMemoize.closeableSupplier(source);
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).allMatch(
				s -> !s.closed && s.count == 1);
		assertThat(count).hasValue(1);

		CloseableClass o = memoized.get();
		assertThat(o).isNotNull();
		assertThat(o.count).isEqualTo(1);
		assertThat(o.closed).isFalse();
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isNotNull();
		assertThat(count).hasValue(1);

		assertThat(catchThrowable(() -> memoized.close())).doesNotThrowAnyException();
		assertThat(o.closed).isTrue();
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		assertThat(catchThrowable(() -> memoized.close())).doesNotThrowAnyException();
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		assertThat(memoized.get()).isNull();
		assertThat(count).hasValue(1);
	}

}
