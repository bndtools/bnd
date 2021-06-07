package aQute.bnd.service.progress;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.service.progress.ProgressPlugin.Task;

/**
 * A central task manager. Background threads should their code with the
 * {@link #with(Task, Callable)} call. This will associate a task with the
 * current thread. Anybody in that thread can call {@link Task#isCanceled()} or
 * cancel that current thread's task.
 * <p>
 * This creates a central point where long running tasks can register their
 * activity so they can be centrally canceled and queried.
 * <p>
 * If no task is active, {@link #isCanceled()} returns false. It is therefore
 * save to call {@link #isCanceled()} at any time.
 */
public abstract class TaskManager {
	final static ThreadLocal<Task>	tasks		= new ThreadLocal<>();
	final static AtomicBoolean		shutdown	= new AtomicBoolean(false);

	/**
	 * Execute a callable keeping the task active on the current thread.
	 *
	 * @param <T> the type of the callable
	 * @param task the task.
	 * @param callable
	 * @return the result of the callable.
	 */
	public static <T> T with(Task task, Callable<T> callable) throws Exception {
		Task prev = tasks.get();
		tasks.set(task);
		try {
			return callable.call();
		} finally {
			tasks.set(prev);
		}
	}

	/**
	 * Answer true if the current thread is associated with a Task and that task
	 * is canceled.
	 *
	 * @return true if the current threads task is canceled, false if there is
	 *         not task or if it is not canceled.
	 */
	public static boolean isCanceled() {
		if (shutdown.get()) {
			return true;
		}
		Task task = tasks.get();
		if (task != null) {
			return task.isCanceled();
		} else
			return false;
	}

	/**
	 * Cancel the current thread's task. Noop if there is no current task
	 */
	public static void cancel() {
		Task task = tasks.get();
		if (task != null) {
			task.abort();
		}
	}

	/**
	 * Shutdown all tasks, cannot be recovered from
	 */
	public static void shtutdown() {
		shutdown.set(true);
	}

	/**
	 * Get the current task if there is one.
	 *
	 * @return the current task or empty
	 */
	public Optional<Task> getTask() {
		return Optional.ofNullable(tasks.get());
	}
}
