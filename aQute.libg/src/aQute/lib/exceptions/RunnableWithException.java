package aQute.lib.exceptions;

/**
 * Runnable interface that allows exceptions.
 */
@FunctionalInterface
public interface RunnableWithException {
	void run() throws Exception;

	default Runnable orElseThrow() {
		return () -> {
			try {
				run();
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		};
	}

	default Runnable ignoreException() {
		return () -> {
			try {
				run();
			} catch (Exception e) {}
		};
	}

	static Runnable asRunnable(RunnableWithException unchecked) {
		return unchecked.orElseThrow();
	}

	static Runnable asRunnableIgnoreException(RunnableWithException unchecked) {
		return unchecked.ignoreException();
	}
}
