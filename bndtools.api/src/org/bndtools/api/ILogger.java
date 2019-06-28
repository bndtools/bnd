package org.bndtools.api;

import org.eclipse.core.runtime.IStatus;

public interface ILogger {

	/**
	 * Log a status message.<br/>
	 * <b>Note:</b> Avoid using this method, better use the other ones
	 *
	 * @param status the status to log
	 */
	void logStatus(IStatus status);

	/**
	 * Log an error message
	 *
	 * @param message the message
	 * @param exception the exception, or null
	 */
	void logError(String message, Throwable exception);

	/**
	 * Log a warning message
	 *
	 * @param message the message
	 * @param exception the exception, or null
	 */
	void logWarning(String message, Throwable exception);

	/**
	 * Log an informational message
	 *
	 * @param message the message
	 * @param exception the exception, or null
	 */
	void logInfo(String message, Throwable exception);

}
