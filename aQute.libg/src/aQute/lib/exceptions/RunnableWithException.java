package aQute.lib.exceptions;

/**
 * Runnable interface that allows exceptions.
 */
@FunctionalInterface
public interface RunnableWithException {
	void run() throws Exception;
}
