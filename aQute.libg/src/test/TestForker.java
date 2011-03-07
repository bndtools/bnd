package test;

import java.util.*;

import junit.framework.*;
import aQute.libg.forker.*;

public class TestForker extends TestCase {

	final Collection<Integer> EMPTY = Collections.emptyList();
	static class R implements Runnable {
		final Collection<Integer> result;
		final int n;
		final Forker<Integer> forker;
		
		R(Collection<Integer> result, int n, Forker<Integer> forker ) {
			this.result = result;
			this.n=n;
			this.forker = forker;
		}
		
		public void run() {
			result.add(n);
			forker.done(n);
		}
	}
	
	public void testSimple() throws InterruptedException {
		final Forker<Integer> forker = new Forker<Integer>();
		final Collection<Integer> result = Collections.synchronizedList( new ArrayList<Integer>());
		
		forker.doWhen( Arrays.asList(3), new R(result, 2,forker));
		forker.doWhen( Arrays.asList(2), new R(result,1,forker));		
		forker.doWhen( EMPTY, new R(result,3,forker));		
		forker.join();
		assertEquals( Arrays.asList(3,2,1), result);		
	}
	
	public void testSimple2() throws InterruptedException {
		final Forker<Integer> forker = new Forker<Integer>();
		final Collection<Integer> result = Collections.synchronizedList( new ArrayList<Integer>());
		
		forker.doWhen( Arrays.asList(1,2,3), new R(result,4,forker));
		forker.doWhen( EMPTY, new R(result,1,forker));
		forker.doWhen( EMPTY, new R(result,2,forker));
		forker.doWhen( EMPTY, new R(result,3,forker));
		forker.join();
		assertEquals( Arrays.asList(1,2,3,4), result);		
	}
}
