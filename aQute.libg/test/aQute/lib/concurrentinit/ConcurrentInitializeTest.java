package aQute.lib.concurrentinit;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class ConcurrentInitializeTest extends Assert {
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

	@Test(expected = IOException.class)
	public void testExceptionForFirstCall() throws Exception {
		ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

			@Override
			public String create() throws Exception {
				throw new IOException();
			}
		};
		ci.get();
	}

	@Test(expected = IOException.class)
	public void testExceptionForFurtherCalls() throws Exception {
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
	}

	@Test(expected = IllegalStateException.class)
	public void testExceptionForNestedCreate() throws Exception {
		ConcurrentInitialize<String> ci = new ConcurrentInitialize<String>() {

			@Override
			public String create() throws Exception {
				return get();
			}
		};
		ci.get();
	}

}
