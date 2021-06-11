package org.bndtools.core.ui;

import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

/**
 * Simple utility to run an runnable always on the display thread.
 */
public class OnDisplayThread {

	/**
	 * Return a Runnable that will be executed directly if the current thread is
	 * the Display thread and otherwise is scheduled on it async.
	 *
	 * @param runnable the runnable to wrap
	 * @return a Runnable that runs the given runnable on the proper thread
	 */
	public static Runnable async(Runnable runnable) {
		return () -> {
			Display display = Display.getDefault();
			if (display.getThread() == Thread.currentThread()) {
				runnable.run();
			} else {
				display.asyncExec(runnable);
			}
		};
	}

	/**
	 * Return a Consumer that will be executed directly if the current thread is
	 * the Display thread and otherwise is scheduled on it async.
	 *
	 * @param consumer the runnable to wrap
	 * @return a Runnable that runs the given runnable on the proper thread
	 */
	public static <T> Consumer<T> async(Consumer<T> consumer) {
		return (T t) -> {
			Display display = Display.getDefault();
			if (display.getThread() == Thread.currentThread()) {
				consumer.accept(t);
			} else {
				display.asyncExec(() -> {
					consumer.accept(t);
				});
			}
		};
	}
}
