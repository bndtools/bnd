package aQute.bnd.service.message;

/**
 * Progress monitor. You can create a progress monitor through
 * {@link Dialogs#createProgress(String)}
 */
public interface Progress extends AutoCloseable {
	/**
	 * Indicate progress
	 *
	 * @param message The subject that is being worked upon
	 * @param percentage Progress in percentage or -1 if unknown
	 * @param timeToFinishInSecs Time to finish the work in seconds or -1 if
	 *            unknown
	 */
	void progress(String message, int percentage, int timeToFinishInSecs);
}
