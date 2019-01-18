package aQute.lib.exceptions;

/**
 * Runnable interface that allows exceptions.
 */
@FunctionalInterface
public interface RunnableWithException {
	void run() throws Exception;

	static Runnable asRunnable(RunnableWithException unchecked) {
		return () -> {
			try {
				unchecked.run();
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}
}
