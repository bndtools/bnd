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
	final Map<T, Job>	waiting		= new HashMap<T, Job>();
	final Set<Job>		executing	= new HashSet<Job>();
	final AtomicBoolean	canceled	= new AtomicBoolean();
	private int			count;

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
				e.printStackTrace();
			} finally {
				synchronized (this) {
					t = null;
				}
				Thread.interrupted(); // clear the interrupt flag
				done(this);
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
	 * @param dependencies
	 *            the dependencies that must have run
	 * @param target
	 *            the target, is removed from all the dependencies when it ran
	 * @param runnable
	 *            the runnable to run
	 */
	public synchronized void doWhen(Collection<? extends T> dependencies, T target,
			Runnable runnable) {
		if (waiting.containsKey(target))
			throw new IllegalArgumentException("You can only add a target once to the forker");

		System.err.println("doWhen " + dependencies + " " + target);
		Job job = new Job();
		job.dependencies = new HashSet<T>(dependencies);
		job.target = target;
		job.runnable = runnable;
		waiting.put(target, job);
	}

	public void start(long ms) throws InterruptedException {
		check();
		count = waiting.size();
		System.err.println("Count " + count);
		schedule();
		if (ms >= 0)
			sync(ms);
	}

	private void check() {
		Set<T> dependencies = new HashSet<T>();
		for (Job job : waiting.values())
			dependencies.addAll(job.dependencies);
		dependencies.removeAll(waiting.keySet());
		if (dependencies.size() > 0)
			throw new IllegalArgumentException(
					"There are dependencies in the jobs that are not present in the targets: "
							+ dependencies);

	}

	public synchronized void sync(long ms) throws InterruptedException {
		System.err.println("Waiting for sync");
		while (count > 0) {
			System.err.println("Waiting for sync " + count);
			wait(ms);
		}
		System.err.println("Exiting sync " + count);
	}

	private void schedule() {
		if (canceled.get())
			return;

		List<Runnable> torun = new ArrayList<Runnable>();
		synchronized (this) {
			for (Iterator<Job> e = waiting.values().iterator(); e.hasNext();) {
				Job job = e.next();
				if (job.dependencies.isEmpty()) {
					torun.add(job);
					executing.add(job);
					e.remove();
				}
			}
		}
		for (Runnable r : torun)
			executor.execute(r);
	}

	/**
	 * Called when the target has ran by the Job.
	 * 
	 * @param done
	 */
	private void done(Job done) {
		synchronized (this) {
			System.err.println("count = " + count);
			executing.remove(done);
			count--;
			if (count == 0) {
				System.err.println("finished");
				notifyAll();
				return;
			}

			for (Job job : waiting.values()) {
				// boolean x =
					job.dependencies.remove(done.target);
				//System.err.println( "Removing " + done.target + " from " + job.target + " ?" + x  + " " + job.dependencies.size());
			}
		}
		schedule();
	}

	/**
	 * Cancel the forker.
	 * 
	 * @throws InterruptedException
	 */
	public void cancel(long ms) throws InterruptedException {
		System.err.println("canceled " + count);

		if (!canceled.getAndSet(true)) {
			synchronized (this) {
				for (Job job : executing) {
					job.cancel();
				}
			}
		}
		sync(ms);
	}

	public int getCount() {
		return count;
	}
}
