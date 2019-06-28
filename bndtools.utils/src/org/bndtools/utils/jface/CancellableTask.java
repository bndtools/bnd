package org.bndtools.utils.jface;

import org.eclipse.jface.operation.IRunnableWithProgress;

public interface CancellableTask extends IRunnableWithProgress {

	/**
	 * Called to signal the cancellation of the task. The task service will call
	 * {@link Thread#interrupt()} on the thread executing the task, but that is
	 * not always enough to interrupt some threads, for example those blocked in
	 * IO. By implementing this method, tasks can perform any special steps
	 * necessary to interrupt properly such as closing a socket.
	 */
	void cancel();

}
