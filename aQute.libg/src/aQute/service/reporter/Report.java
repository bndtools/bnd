package aQute.service.reporter;

import java.util.List;

/**
 * A base interface to represent the state of a work in progress.
 */
public interface Report {
	/**
	 * Defines a record for the location of an error/warning
	 */
	class Location {
		public String	message;
		public int		line;
		public String	file;
		public String	header;
		public String	context;
		public String	reference;
		public String	methodName;
		/**
		 * @see Reporter.SetLocation#details(Object)
		 */
		public Object	details;
		public int		length;

		public Location dup() {
			Location l = new Location();
			l.message = message;
			l.line = line;
			l.file = file;
			l.context = context;
			l.reference = reference;
			l.methodName = methodName;
			l.details = details;
			l.length = length;
			return l;
		}

		/**
		 * Copies the location details to the destination. This will not include
		 * the message.
		 *
		 * @param destination the other location.
		 */
		public void to(Location destination) {
			destination.line = line;
			destination.file = file;
			destination.context = context;
			destination.reference = reference;
			destination.methodName = methodName;
			destination.details = details;
			destination.length = length;
		}
	}

	/**
	 * Return the warnings. This list must not be changed and may be immutable.
	 *
	 * @return the warnings
	 */
	List<String> getWarnings();

	/**
	 * Return the errors. This list must not be changed and may be immutable.
	 *
	 * @return the errors
	 */
	List<String> getErrors();

	/**
	 * Return the errors for the given error or warning. Can return null.
	 *
	 * @param msg The message
	 * @return null or the location of the message
	 */
	Location getLocation(String msg);

	/**
	 * Check if this report has any relevant errors that should make the run
	 * associated with this report invalid. I.e. if this returns false then the
	 * run should be disregarded.
	 *
	 * @return true if this run should be disregarded due to errors
	 */

	boolean isOk();
}
