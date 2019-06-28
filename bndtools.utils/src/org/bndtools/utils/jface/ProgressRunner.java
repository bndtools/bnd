package org.bndtools.utils.jface;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

/**
 * This class aims to improve the bedlam of Eclipse's progress dialogs that
 * don't interrupt the threads, thus requiring us to constantly poll the
 * {@link IProgressMonitor#isCanceled()} method, which is not generally possible
 * if we are doing IO or other blocking tasks.
 */
public class ProgressRunner {

	/**
	 * <p>
	 * Execute the specified runnable within the runnable context. The work is
	 * done on a separate thread, and if the user cancels the operation then the
	 * work thread will be interrupted with {@link Thread#interrupt()}. If
	 * custom cancellation logic is required then the provided runnable can
	 * implement {@link CancellableTask} instead of {@link IProgressMonitor}
	 * directly.
	 * </p>
	 * <p>
	 * This method is synchronous and returns when the runnable completes,
	 * whether normally or cancellation or error.
	 * </p>
	 *
	 * @param fork
	 * @param runnable
	 * @param context
	 * @param display
	 * @throws InvocationTargetException
	 */
	public static void execute(boolean fork, final IRunnableWithProgress runnable, IRunnableContext context,
		final Display display) throws InvocationTargetException {
		try {
			context.run(fork, true, monitor -> {
				OffGuiThreadProgressMonitor delegatingMonitor = new OffGuiThreadProgressMonitor(display, monitor);
				ProgressRunnerThread thread = new ProgressRunnerThread(runnable, delegatingMonitor);
				thread.start();

				if (display.getThread() == Thread.currentThread()) {
					// We are running unforked on the display thread, so keep it
					// alive and processing events while
					// we ping the cancellation status.
					while (thread.isAlive()) {
						if (!display.readAndDispatch()) {
							if (monitor.isCanceled()) {
								thread.interrupt();
							}
							display.sleep();
						}
					}
				} else {
					// We are forked on another thread, so just sleep in between
					// pings.
					while (thread.isAlive()) {
						if (monitor.isCanceled())
							thread.interrupt();
						Thread.sleep(500);
					}
				}

				InvocationTargetException exception = thread.exception;
				if (exception != null)
					throw exception;
			});
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private static class ProgressRunnerThread extends Thread {

		private final IRunnableWithProgress			runnable;
		private final IProgressMonitor				monitor;

		private volatile InvocationTargetException	exception	= null;

		public ProgressRunnerThread(IRunnableWithProgress runnable, IProgressMonitor monitor) {
			super("Progress Runner");
			this.runnable = runnable;
			this.monitor = monitor;
		}

		@Override
		public void run() {
			try {
				runnable.run(monitor);
			} catch (InvocationTargetException e) {
				this.exception = e;
			} catch (InterruptedException e) {
				// ignore
			}
		}

		@Override
		public void interrupt() {
			try {
				if (runnable instanceof CancellableTask)
					((CancellableTask) runnable).cancel();
			} finally {
				super.interrupt();
			}
		}

	}

	private static class OffGuiThreadProgressMonitor implements IProgressMonitor {

		private final Display			display;
		private final IProgressMonitor	delegate;

		public OffGuiThreadProgressMonitor(Display display, IProgressMonitor delegate) {
			this.display = display;
			this.delegate = delegate;
		}

		@Override
		public void beginTask(final String name, final int totalWork) {
			display.asyncExec(() -> delegate.beginTask(name, totalWork));
		}

		@Override
		public void done() {
			display.asyncExec(() -> delegate.done());
		}

		@Override
		public void internalWorked(final double work) {
			display.asyncExec(() -> delegate.internalWorked(work));
		}

		@Override
		public boolean isCanceled() {
			return Thread.currentThread()
				.isInterrupted();
		}

		@Override
		public void setCanceled(boolean value) {}

		@Override
		public void setTaskName(final String name) {
			display.asyncExec(() -> delegate.setTaskName(name));
		}

		@Override
		public void subTask(final String name) {
			display.asyncExec(() -> delegate.subTask(name));
		}

		@Override
		public void worked(final int work) {
			display.asyncExec(() -> delegate.worked(work));
		}
	}

}
