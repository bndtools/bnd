package aQute.bnd.service.message;

import java.util.regex.Pattern;

import org.osgi.util.promise.Promise;

import aQute.service.reporter.Reporter;

/**
 * A simple dialog handler for prompting for an input string from the user,
 * displaying a message to the user, or showing a set of errors and warnings.
 * These methods can be called on any thread.
 */
public interface Dialogs {

	/**
	 * Display a message and optionally a set of buttons. Return the index of
	 * the selected button or -1 if the dialog was canceled.
	 *
	 * @param title The title of the dialog, must not be null
	 * @param message The message displayed, must not be null
	 * @param buttons A list of buttons, may be null
	 * @param defaultIndex The default index. Must be -1 for none or between
	 *            0..n, where n is the number of buttons specified.
	 * @return -1 if canceled, otherwise the index of the given button that was
	 *         pressed.
	 */
	Promise<Integer> message(String title, String message, String[] buttons, int defaultIndex) throws Exception;

	/**
	 * Display a dialog where the user can input a string. An initial value can
	 * be supplied and a validator can optionally beused to validate any input.
	 * Either null is returned when the user cancels or a string with valid
	 * input.
	 *
	 * @param title The title of the dialog, must not be null
	 * @param query The query displayed, must not be null
	 * @param initialValue The initial value, can be null
	 * @param validator A pattern that must match the input, can be null if no
	 *            validator is necessary
	 * @return A string object or null if no input was given
	 */
	Promise<String> prompt(String title, String query, String initialValue, Pattern validator) throws Exception;

	/**
	 * Display a list of errors and warnings. This method will return
	 * immediately, it will not wait for the user to dismiss this window.
	 * Multiple calls might actually be aggregated into a single dialog
	 *
	 * @param message The message displayed, must not be null
	 * @param reporter Contains the errors and warnings
	 */
	void errors(String message, Reporter reporter) throws Exception;

	/**
	 * Create a progress monitor
	 */

	Progress createProgress(String title) throws Exception;

}
