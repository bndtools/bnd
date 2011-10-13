package aQute.libg.forker;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A Forker is good in parallel scheduling tasks with dependencies. You can add
 * tasks with {@link #doWhen(Collection, Object, Runnable)}. The collection is
 * the list of dependencies, the object is the target, and the runnable is run
 * to update the target. The runnable will only run when all its dependencies
 * have ran their associated runnable.
 * 
 * @author aqute
 * 
 * @param <T>
 */
public class Forker<T> {
	final Executor		executor;
	final Set<T>		done		= new HashSet<T>();
	final List<Job>		waiting		= new ArrayList<Job>();
	final Semaphore		semaphore	= new Semaphore(0);
	final AtomicInteger	outstanding	= new AtomicInteger();
	final AtomicBoolean	canceled	= new AtomicBoolean();

	/**
	 * Helper class to model a Job
	 */
	class Job implements Runnable {
		T						target;
		Set<T>					dependencies;
		Runnable				runnable;
		Throwable				exception;
		volatile Thread			t;
		volatile AtomicBoolean	canceled	= new AtomicBoolean(false);

		/**
		 * Run when the job's dependencies are done.
		 */
		public void run() {
			Thread.interrupted(); // clear the interrupt flag

			try {
				synchronized (this) {
					// Check if we got canceled
					if (canceled.get())
						return;

					t = Thread.currentThread();
				}
				runnable.run();
			} catch (Exception e) {
				exception = e;
			} finally {
				synchronized (this) {
					t = null;
				}
				Thread.interrupted(); // clear the interrupt flag
				done(target);
			}
		}

		/**
		 * Cancel this job
		 */
		private void cancel() {
			if (!canceled.getAndSet(true)) {
				synchronized (this) {
					if (t != null)
						t.interrupt();
				}
			}
		}
	}

	/**
	 * Constructor
	 * 
	 * @param executor
	 */
	public Forker(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Constructor
	 * 
	 */
	public Forker() {
		this.executor = Executors.newFixedThreadPool(4);
	}

	/**
	 * Schedule a job for execution when the dependencies are done of target are
	 * done.
	 * 
	 * @param dependencies the dependencies that must have run
	 * @param target the target, is removed from all the dependencies when it ran
	 * @param runnable the runnable to run
	 */
	public synchronized void doWhen(Collection<? extends T> dependencies, T target,
			Runnable runnable) {
		System.out.println("doWhen " + dependencies);
		outstanding.incrementAndGet();
		Job job = new Job();
		job.dependencies = new HashSet<T>(dependencies);
		job.dependencies.removeAll(done);
		job.target = target;

		job.runnable = runnable;
		if (job.dependencies.isEmpty()) {
			executor.execute(job);
		} else {
			waiting.add(job);
		}
	}

	/**
	 * Called when the target has ran by the Job.
	 * 
	 * @param done
	 */
	private void done(T done) {
		List<Runnable> torun = new ArrayList<Runnable>();
		synchronized (this) {
			System.out.println("done " + done);
			semaphore.release();

			for (Iterator<Job> e = waiting.iterator(); e.hasNext();) {
				Job job = e.next();
				if (job.dependencies.remove(done) && job.dependencies.isEmpty()) {
					System.out.println("scheduling " + job.target);
					torun.add(job);
					e.remove();
				}
			}
		}
		for (Runnable r : torun)
			executor.execute(r);
	}

	/**
	 * Wait until all jobs have run.
	 * 
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException {
		System.out.println("join " + outstanding + " " + semaphore);
		check();
		semaphore.acquire(outstanding.getAndSet(0));
	}

	/**
	 * Check that we have no jobs that can never be satisfied. I.e. if 
	 * the dependencies contain a target that is not listed.
	 */
	private void check() {
		// TODO
	}

	/**
	 * Return the number of outstanding jobs
	 * @return outstanding jobs
	 */
	public int getOutstanding() {
		return semaphore.availablePermits();
	}

	/**
	 * Cancel the forker.
	 * 
	 * @throws InterruptedException
	 */
	public void cancel() throws InterruptedException {
		System.out.println("canceled " + outstanding + " " + semaphore);

		if (!canceled.getAndSet(true)) {
			for (Job job : waiting) {
				job.cancel();
			}
		}
		join();
	}
}
