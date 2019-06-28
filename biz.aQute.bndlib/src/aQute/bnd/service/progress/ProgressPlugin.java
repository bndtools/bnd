package aQute.bnd.service.progress;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A plugin for reporting progress on long-running jobs.
 *
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
@ProviderType
public interface ProgressPlugin {

	/**
	 * Start a task with the specified name.
	 *
	 * @param name The name of the task -- must not be null.
	 * @param size The expected size of the task, or -1 if not known in advance.
	 * @return A handle for the ongoing task.
	 */
	Task startTask(String name, int size);

	/**
	 * Represents an ongoing task.
	 */
	@ProviderType
	interface Task {
		/**
		 * The specified number of units out of the total have been worked. If
		 * called after {@code done()}, an {@link IllegalStateException} may be
		 * thrown.
		 *
		 * @param units
		 */
		void worked(int units);

		/**
		 * The task has been completed; optionally with a message and/or
		 * exception to indicate the outcome. After calling this method, no more
		 * work should be performed in the task.
		 *
		 * @param message A message associated with the completion of the task;
		 *            may be null.
		 * @param e An exception thrown by the task; may be null;
		 */
		void done(String message, Throwable e);

		/**
		 * Check if this task is canceled.
		 */
		boolean isCanceled();
	}

}
