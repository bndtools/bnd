package aQute.libg.forker;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Forker<T> {
	final Executor executor;
	final Set<T>	done	= new HashSet<T>();
	final List<Job>	waiting	= new ArrayList<Job>();
	final Semaphore semaphore = new Semaphore(0);
	final AtomicInteger outstanding = new AtomicInteger();
	
	class Job {
		Set<T>		dependencies;
		Runnable	runnable;
	}


	
	public Forker(Executor executor) {
		this.executor = executor;
	}
	
	public Forker() {
		this.executor = Executors.newFixedThreadPool( 4);
	}
	
	
	
	
	public synchronized void doWhen(Collection<? extends T> dependencies, Runnable runnable) {
		System.out.println("doWhen " + dependencies );
		outstanding.incrementAndGet();
		Job job = new Job();
		job.dependencies = new HashSet<T>(dependencies);
		job.dependencies.removeAll(done);
		if ( job.dependencies.isEmpty())
			executor.execute(runnable);
		else {
			job.runnable = runnable;
			waiting.add(job);
		}
	}

	public synchronized void done( T done ) {
		System.out.println("done " + done);
		semaphore.release();
		for ( Job job : waiting ) {
			if ( job.dependencies.remove(done) && job.dependencies.isEmpty() ) {
				System.out.println("scheduling " + job.dependencies);
				executor.execute(job.runnable);
			}
		}
	}
	


	public void join() throws InterruptedException {
		System.out.println("join " + outstanding + " " + semaphore);
		semaphore.acquire(outstanding.getAndSet(0));
	}

}
