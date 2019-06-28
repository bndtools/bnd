package aQute.bnd.service.diff;

/**
 * The Delta provides information about the {@link Diff} object. It tells the
 * relation between the newer and older compared elements.
 */
public enum Delta {

	// ORDER IS IMPORTANT FOR TRANSITIONS TABLE!

	/**
	 *
	 */
	IGNORED, // for all
	UNCHANGED,
	CHANGED,
	MICRO,
	MINOR,
	MAJOR, // content
	REMOVED,
	ADDED; // structural

}
