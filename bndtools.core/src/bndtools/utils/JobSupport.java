package bndtools.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import aQute.bnd.exceptions.ConsumerWithException;
import aQute.bnd.exceptions.FunctionWithException;
import bndtools.Plugin;

public class JobSupport {
	public static <T> void background(String message, FunctionWithException<IProgressMonitor, ? extends T> background,
		ConsumerWithException<? super T> onDisplayThread) {

		Job job = new Job(message) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					T result = background.apply(monitor);
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					Display.getDefault()
						.asyncExec(() -> {
							try {
								onDisplayThread.accept(result);
							} catch (Exception e) {
							}
						});
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e);
				}
			}
		};
		job.schedule();
	}

}
