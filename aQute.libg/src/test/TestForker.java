package test;

import java.util.*;

import junit.framework.*;
import aQute.libg.forker.*;

public class TestForker extends TestCase {

	final Collection<Integer> EMPTY = Collections.emptyList();
	
	static class R implements Runnable {
		final Collection<Integer> result;
		final int n;
		
		R(Collection<Integer> result, int n) {
			this.result = result;
			this.n=n;
		}
		
		public void run() {
			result.add(n);
		}
	}
	
	public void testSimple() throws InterruptedException {
		final Forker<Integer> forker = new Forker<Integer>();
		final Collection<Integer> result = Collections.synchronizedList( new ArrayList<Integer>());
		
		forker.doWhen( Arrays.asList(3), 2, new R(result, 2));
		forker.doWhen( Arrays.asList(2), 1, new R(result,1));		
		forker.doWhen( EMPTY, 3, new R(result,3));		
		forker.join();
		assertEquals( Arrays.asList(3,2,1), result);		
	}
	
	public void testSimple2() throws InterruptedException {
		final Forker<Integer> forker = new Forker<Integer>();
		final Collection<Integer> result = Collections.synchronizedList( new ArrayList<Integer>());
		
		forker.doWhen( Arrays.asList(1,2,3), 4, new R(result,4));
		forker.doWhen( EMPTY, 1, new R(result,1));
		forker.doWhen( EMPTY, 2, new R(result,2));
		forker.doWhen( EMPTY, 3, new R(result,3));
		forker.join();
		assertEquals( Arrays.asList(1,2,3,4), result);		
	}
	
	
	public void testCancel() throws InterruptedException {
		final Forker<Integer> forker = new Forker<Integer>();
		final Collection<Integer> result = Collections.synchronizedList( new ArrayList<Integer>());
		
		forker.doWhen( EMPTY, 4, new Runnable() {

			public void run() {
				synchronized(result) {
					try {
						System.out.println("starting to wait");
						wait();
						System.out.println("finished wait");
					} catch (InterruptedException e) {
						System.out.println("exception");
						e.printStackTrace();
					} finally {
						System.out.println("leaving task");						
					}
				}
			}
			
		});
		Thread.sleep(100);
		assertEquals(1, forker.getOutstanding());
		forker.cancel();
		assertEquals(0, forker.getOutstanding());
	}
}
