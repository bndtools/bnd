package aQute.lib.concurrent.serial;

/**
 * Trigger an action and repeat it as long as it keeps being triggered. General
 * purpose is to start a background thread but let it keep repeating until there
 * no more triggers were received in the mean time.
 * <p>
 * For example, you detect a change in the file system and then need to
 * synchronize a persistent structure. The {@link #trigger()} call will return
 * the true the first time, indicating that a background thread should be
 * started. The background tasks does it works and checks the {@link #doit()} at
 * the end. If it returns true, it restarts, else it returns.
 * <p>
 */
public class TriggerRepeat {
	int	triggers;
	int	doits;

	/**
	 * Trigger the action, returns true if there was no action.
	 *
	 * @return true if there was no action
	 */
	public synchronized boolean trigger() {
		boolean result = triggers == doits;
		triggers++;
		return result;
	}

	/**
	 * Query if the action should be executed. The action should be executed if
	 * there are have been more trigger() calls after the doit() call. After
	 * this call, triggers == doits. I.e. it coalesces multiple triggers.
	 *
	 * @return true of the the triggers != dots
	 */

	public synchronized boolean doit() {
		boolean result = triggers != doits;
		doits = triggers;
		return result;
	}

}
