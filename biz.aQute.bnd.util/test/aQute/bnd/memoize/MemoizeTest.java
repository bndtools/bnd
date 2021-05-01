package aQute.bnd.memoize;

import static aQute.bnd.exceptions.ConsumerWithException.asConsumer;
import static aQute.bnd.exceptions.RunnableWithException.asRunnable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.Closeable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
		Memoize<String> memoized = Memoize.refreshingSupplier(source, 300, TimeUnit.MILLISECONDS);
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("1");
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isEqualTo("1");
		assertThat(count).hasValue(1);

		sleep(400, TimeUnit.MILLISECONDS);

		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly("2");
		assertThat(count).hasValue(2);
		assertThat(memoized.peek()).isEqualTo("2");
		assertThat(count).hasValue(2);

		sleep(400, TimeUnit.MILLISECONDS);

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

		assertThatNullPointerException().isThrownBy(() -> memoized.accept(null));
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

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
		AtomicInteger	closed;
		final int		count;

		CloseableClass(int count) {
			this.count = count;
			closed = new AtomicInteger();
		}

		@Override
		public void close() {
			closed.incrementAndGet();
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
			.limit(100)).allMatch(s -> s.closed.get() == 0 && s.count == 1);
		assertThat(count).hasValue(1);

		CloseableClass o = memoized.get();
		assertThat(o).isNotNull();
		assertThat(o.count).isEqualTo(1);
		assertThat(o.closed).hasValue(0);
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isNotNull();
		assertThat(count).hasValue(1);

		assertThat(catchThrowable(memoized::close)).doesNotThrowAnyException();
		assertThat(o.closed).hasValue(1);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		// close
		assertThat(catchThrowable(memoized::close)).doesNotThrowAnyException();
		assertThat(memoized.peek()).isNull();
		assertThat(o.closed).hasValue(1);
		assertThat(count).hasValue(1);
		assertThatIllegalStateException().isThrownBy(memoized::get);

		// close again
		assertThat(catchThrowable(memoized::close)).doesNotThrowAnyException();
		assertThat(o.closed).hasValue(1);
		assertThat(count).hasValue(1);
	}

	@Test
	public void acceptWhileCloseIsBlocked() {
		CloseableMemoize<Closeable> memoized = CloseableMemoize.closeableSupplier(() -> () -> {});
		CallOnOtherThread s = new CallOnOtherThread(memoized::close, 2000);
		memoized.accept(c -> {
			s.call();
			assertThat(s.hasEnded()).isFalse();
		});
		assertThat(s.hasEnded()).isTrue();
	}

	@Test
	public void exceptionWhileInsideClose() throws InterruptedException {
		Semaphore makeCloseWait = new Semaphore(0);
		CloseableMemoize<Closeable> memoized = CloseableMemoize.closeableSupplier(() -> () -> {
			try {
				makeCloseWait.acquire();
			} catch (InterruptedException e) {}
		});

		CallOnOtherThread close = new CallOnOtherThread(memoized::close, 2000);
		assertThat(memoized.get()).isNotNull();
		assertThat(memoized.isClosed()).isFalse();
		assertThat(memoized.peek()).isNotNull();

		close.call();

		assertThat(close.hasEnded()).isFalse();
		makeCloseWait.release();
		assertThat(close.hasEnded()).isTrue();

		assertThat(memoized.isClosed()).isTrue();
		assertThat(memoized.peek()).isNull();
		assertThatIllegalStateException().isThrownBy(memoized::get);
	}

	@Test
	public void closeable_initial() throws Exception {
		Supplier<CloseableClass> source = () -> new CloseableClass(count.incrementAndGet());
		CloseableMemoize<CloseableClass> memoized = CloseableMemoize.closeableSupplier(source);
		assertThat(count).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(catchThrowable(memoized::close)).doesNotThrowAnyException();
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThat(catchThrowable(memoized::close)).doesNotThrowAnyException();
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		assertThatIllegalStateException().isThrownBy(memoized::get);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);
	}

	@Test
	public void closeable_null() {
		assertThatNullPointerException()
			.isThrownBy(() -> CloseableMemoize.closeableSupplier((Supplier<AutoCloseable>) null));
		CloseableMemoize<AutoCloseable> memoized = CloseableMemoize
			.closeableSupplier((Supplier<AutoCloseable>) () -> null);
		assertThatIllegalStateException().isThrownBy(memoized::get);
		assertThat(memoized.isClosed()).isTrue();
		assertThat(memoized.peek()).isNull();
	}

	@SuppressWarnings("resource")
	@Test
	public void closeable_accept() throws Exception {
		Supplier<CloseableClass> source = () -> new CloseableClass(count.incrementAndGet());
		CloseableMemoize<CloseableClass> memoized = CloseableMemoize.closeableSupplier(source);

		assertThatNullPointerException().isThrownBy(() -> memoized.accept(null));
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);

		AtomicReference<CloseableClass> value = new AtomicReference<>();

		assertThat(catchThrowable(() -> memoized.accept(value::set))).doesNotThrowAnyException();

		CloseableClass o = value.get();
		assertThat(o).isNotNull();
		assertThat(o.count).isEqualTo(1);
		assertThat(o.closed).hasValue(0);
		assertThat(count).hasValue(1);
		assertThat(memoized.peek()).isNotNull();
		assertThat(count).hasValue(1);

		assertThat(catchThrowable(memoized::close)).doesNotThrowAnyException();
		assertThat(o.closed).hasValue(1);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(1);

		assertThatIllegalStateException().isThrownBy(() -> memoized.accept(value::set));
		assertThat(memoized.peek()).isNull();
		assertThat(o.closed).hasValue(1);
		assertThat(count).hasValue(1);
		assertThat(value).hasValue(o);

	}

	@Test
	public void closeable_accept_multi() throws Exception {
		final int multi = 5;
		Supplier<CloseableClass> source = () -> new CloseableClass(count.incrementAndGet());
		ExecutorService threadPool = Executors.newFixedThreadPool(multi);
		try (CloseableMemoize<AutoCloseable> memoized = CloseableMemoize.closeableSupplier(source)) {
			CountDownLatch consumerReady = new CountDownLatch(multi);
			CountDownLatch consumerSync = new CountDownLatch(1);
			CountDownLatch consumerDone = new CountDownLatch(multi);
			Consumer<AutoCloseable> consumer = asConsumer(s -> {
				consumerReady.countDown();
				if (consumerSync.await(20, TimeUnit.SECONDS)) {
					memoized.accept(x -> consumerDone.countDown());
				}
			});

			CountDownLatch threadReady = new CountDownLatch(multi);
			CountDownLatch threadSync = new CountDownLatch(1);
			for (int i = 0; i < multi; i++) {
				threadPool.execute(asRunnable(() -> {
					threadReady.countDown();
					if (threadSync.await(20, TimeUnit.SECONDS)) {
						memoized.accept(consumer);
					}
				}));
			}
			assertThat(threadReady.await(10, TimeUnit.SECONDS))
				.as("%s threads ready: count %s", multi, threadReady.getCount())
				.isTrue();
			threadSync.countDown();
			assertThat(consumerReady.await(10, TimeUnit.SECONDS))
				.as("%s consumers ready: count %s", multi, consumerReady.getCount())
				.isTrue();
			consumerSync.countDown();
			assertThat(consumerDone.await(10, TimeUnit.SECONDS))
				.as("%s consumers done: count %s", multi, consumerDone.getCount())
				.isTrue();
			assertThat(count).hasValue(1);
		} finally {
			threadPool.shutdown();
		}
	}

	@Test
	public void predicate_supplier() {
		Memoize<String> memoized = Memoize.predicateSupplier(source, Objects::nonNull);
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
	public void predicate_supplier_someaccepted() {
		source = () -> {
			int n = count.incrementAndGet();
			if (n <= 10) {
				return null;
			}
			return Integer.toString(n);
		};
		AtomicInteger acceptorCount = new AtomicInteger();
		Predicate<String> acceptor = s -> {
			acceptorCount.incrementAndGet();
			return Objects.nonNull(s);
		};
		Memoize<String> memoized = Memoize.predicateSupplier(source, acceptor);
		assertThat(count).hasValue(0);
		assertThat(acceptorCount).hasValue(0);
		assertThat(memoized.peek()).isNull();
		assertThat(count).hasValue(0);
		assertThat(acceptorCount).hasValue(0);

		assertThat(Stream.generate(memoized::get)
			.limit(10)).containsOnly((String) null);
		assertThat(count).hasValue(10);
		assertThat(acceptorCount).hasValue(10);
		assertThat(Stream.generate(memoized::get)
			.limit(100)).containsOnly("11");
		assertThat(count).hasValue(11);
		assertThat(acceptorCount).hasValue(11);
		assertThat(memoized.peek()).isEqualTo("11");
		assertThat(count).hasValue(11);
		assertThat(acceptorCount).hasValue(11);
	}

	@Test
	public void predicate_supplier_null() {
		assertThatNullPointerException().isThrownBy(() -> Memoize.predicateSupplier(null, null));
		assertThatNullPointerException().isThrownBy(() -> Memoize.predicateSupplier(source, null));
		assertThatNullPointerException().isThrownBy(() -> Memoize.predicateSupplier(null, Objects::nonNull));
	}

}
