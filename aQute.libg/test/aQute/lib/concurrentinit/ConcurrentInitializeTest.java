package aQute.lib.concurrentinit;

import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class ConcurrentInitializeTest {
	final static Executor executor = Executors.newCachedThreadPool();

	@Test
	public void testSimple() throws Exception {
		ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

			@Override
			public String create() throws Exception {
				return "foo";
			}
		};
		assertEquals("foo", ci.get());
	}

	@Test
	public void testConcurrent() throws InterruptedException {
		final Semaphore result = new Semaphore(0);
		final Semaphore onCreate = new Semaphore(0);
		final ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

			@Override
			public String create() throws Exception {
				onCreate.acquire();
				return "foo";
			}
		};
		for (int i = 0; i < 10; i++) {
			executor.execute(() -> {
				try {
					ci.get();
					result.release();
				} catch (Exception e) {
					// cant happen
				}
			});
		}
		// First wait so that we can verify that nobody can
		// continue until we allow it
		Thread.sleep(100);
		assertEquals(0, result.availablePermits());

		// allow the creator to pass
		onCreate.release(1); // only 1 can create

		// Check the results
		assertTrue(result.tryAcquire(10, 10, TimeUnit.SECONDS));
	}

	@Test
	public void testExceptionForFirstCall() throws Exception {
		assertThatIOException().isThrownBy(() -> {
			ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

				@Override
				public String create() throws Exception {
					throw new IOException();
				}
			};
			ci.get();
		});
	}

	@Test
	public void testExceptionForFurtherCalls() throws Exception {
		assertThatIOException().isThrownBy(() -> {
			ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

				@Override
				public String create() throws Exception {
					throw new IOException();
				}
			};
			try {
				ci.get();
			} catch (IllegalArgumentException e) {
				// ignore
			}
			ci.get();
		});
	}

	@Test
	public void testExceptionForNestedCreate() throws Exception {
		assertThatIllegalStateException().isThrownBy(() -> {
			ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

				@Override
				public String create() throws Exception {
					return get();
				}
			};
			ci.get();
		});
	}

}
