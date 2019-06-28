package aQute.libg.forker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

public class TestForker extends TestCase {

	final Collection<Integer>	EMPTY	= Collections.emptyList();
	final int					TIMEOUT	= 1000000;

	static class R implements Runnable {
		final Collection<Integer>	result;
		final int					n;

		R(Collection<Integer> result, int n) {
			this.result = result;
			this.n = n;
		}

		@Override
		public void run() {
			result.add(n);
		}
	}

	public void testSimple() throws InterruptedException {
		final Forker<Integer> forker = new Forker<>();
		final Collection<Integer> result = Collections.synchronizedList(new ArrayList<Integer>());

		forker.doWhen(Arrays.asList(3), 2, new R(result, 2));
		forker.doWhen(Arrays.asList(2), 1, new R(result, 1));
		forker.doWhen(EMPTY, 3, new R(result, 3));
		forker.start(1000);
		assertEquals(Arrays.asList(3, 2, 1), result);
	}

	public void testSimple2() throws InterruptedException {
		final Forker<Integer> forker = new Forker<>();
		final Collection<Integer> result = Collections.synchronizedList(new ArrayList<Integer>());

		forker.doWhen(Arrays.asList(1, 2, 3), 4, new R(result, 4));
		forker.doWhen(EMPTY, 1, new R(result, 1));
		forker.doWhen(EMPTY, 2, new R(result, 2));
		forker.doWhen(EMPTY, 3, new R(result, 3));
		forker.start(1000);

		assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4)));
		Object[] q = result.toArray();
		assertEquals(Integer.valueOf(4), q[q.length - 1]);
	}

	public void testInvalid() {
		final Forker<Integer> forker = new Forker<>();
		final Collection<Integer> result = Collections.synchronizedList(new ArrayList<Integer>());
		forker.doWhen(Arrays.asList(1, 2, 3), 4, new R(result, 4));
		try {
			forker.start(100);
			fail();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			assertEquals(IllegalArgumentException.class, e.getClass());
		}

	}

	public void testCancel() throws InterruptedException {
		final Forker<Integer> forker = new Forker<>();
		final Collection<Integer> result = Collections.synchronizedList(new ArrayList<Integer>());

		forker.doWhen(EMPTY, 4, () -> {
			synchronized (result) {
				try {
					System.err.println("starting to wait");
					result.wait(TIMEOUT);
					System.err.println("finished wait");
				} catch (Exception e) {
					System.err.println("exception");
					e.printStackTrace();
				} finally {
					System.err.println("leaving task");
				}
			}
		});
		forker.start(-1);
		Thread.sleep(1000);
		assertEquals(1, forker.getCount());
		forker.cancel(1000);
		assertEquals(0, forker.getCount());
	}
}
