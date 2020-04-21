package aQute.lib.memoize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class MemoizeTest {

	@Test
	public void supplier() {
		AtomicInteger count = new AtomicInteger();
		Supplier<String> source = () -> Integer.toString(count.incrementAndGet());
		Supplier<String> memoized = Memoize.supplier(source);
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("1");
		assertThat(count).hasValue(1);
	}

	@Test
	public void supplier_null() {
		assertThatNullPointerException().isThrownBy(() -> Memoize.supplier(null));
	}

	@Test
	public void supplier_function() {
		Function<AtomicInteger, String> source = count -> Integer.toString(count.incrementAndGet());
		AtomicInteger count = new AtomicInteger();
		Supplier<String> memoized = Memoize.supplier(source, count);
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("1");
		assertThat(count).hasValue(1);
	}

	@Test
	public void supplier_function_null() {
		assertThatNullPointerException()
			.isThrownBy(() -> Memoize.supplier((Function<Object, Object>) null, new Object()));
	}

	@Test
	public void refreshing() throws Exception {
		AtomicInteger count = new AtomicInteger();
		Supplier<String> source = () -> Integer.toString(count.incrementAndGet());
		Supplier<String> memoized = Memoize.refreshingSupplier(source, 100, TimeUnit.MILLISECONDS);
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("1");
		assertThat(count).hasValue(1);

		sleep(200, TimeUnit.MILLISECONDS);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("2");
		assertThat(count).hasValue(2);

		sleep(200, TimeUnit.MILLISECONDS);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("3");
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
		AtomicInteger count = new AtomicInteger();
		Supplier<String> source = () -> Integer.toString(count.incrementAndGet());
		ReferenceQueue<String> queue = new ReferenceQueue<>();
		Supplier<String> memoized = Memoize.referenceSupplier(source, t -> new WeakReference<>(t, queue));
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("1");
		assertThat(count).hasValue(1);

		System.gc();
		assertThat(queue.remove(1000)).isNotNull();

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("2");
		assertThat(count).hasValue(2);

		System.gc();
		assertThat(queue.remove(1000)).isNotNull();

		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("3");
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

}
