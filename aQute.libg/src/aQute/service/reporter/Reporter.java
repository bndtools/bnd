package aQute.service.reporter;

/**
 * A base interface to model a work in progress. Though exceptions work well for
 * major, well, exceptions, they are lousy for reporting errors/warnings in a
 * task. Logging also sucks because it is global, hard to relate to a single
 * piece of work. This small (uncoupled) interface is intended to fill this gap.
 * The idea is that different tasks can perform parts and the progress can be
 * integrated. A reporter is not mandated to track locations. Locations should
 * be seen as best effort.
 */
public interface Reporter extends Report {
	/**
	 * Fluid interface to set location data
	 */
	interface SetLocation {
		/**
		 * Set the file location
		 */
		SetLocation file(String file);

		/**
		 * Set the header/section location. This is normally the header in a
		 * manifest or properties file.
		 */
		SetLocation header(String header);

		/**
		 * Set the context in the header.
		 *
		 * @param context
		 */
		SetLocation context(String context);

		/**
		 * Set the method where the error is reported.
		 *
		 * @param methodName
		 */
		SetLocation method(String methodName);

		/**
		 * Set the line number. Line 0 is the top line.
		 */
		SetLocation line(int n);

		/**
		 * Set a reference for the error (url or so)
		 */

		SetLocation reference(String reference);

		/**
		 * Pass a DTO containing detailed information about the error. This can
		 * be recognised by other tools (e.g. bndtools) and used for further
		 * error reporting/fixing.
		 */
		SetLocation details(Object details);

		Location location();

		SetLocation length(int length);
	}

	/**
	 * Create an error. Implementations must ensure that the given args are not
	 * prevented from garbage collecting. The args must have a proper toString
	 * method.
	 *
	 * @param format The format of the error
	 * @param args The arguments of the error
	 * @return a SetLocation to set the location
	 */
	SetLocation error(String format, Object... args);

	/**
	 * Create a warning. Implementations must ensure that the given args are not
	 * prevented from garbage collecting. The args must have a proper toString
	 * method.
	 *
	 * @param format The format of the error
	 * @param args The arguments of the error
	 * @return a SetLocation to set the location
	 */
	SetLocation warning(String format, Object... args);

	/**
	 * Create a warning. Implementations must ensure that the given args are not
	 * prevented from garbage collecting. The args must have a proper toString
	 * method.
	 *
	 * @param format The format of the error
	 * @param args The arguments of the error
	 */
	void trace(String format, Object... args);

	/**
	 * Create a warning. Implementations must ensure that the given args are not
	 * prevented from garbage collecting. The args must have a proper toString
	 * method.
	 *
	 * @param progress A value between 0 and 1 indicating the progress. 0 is
	 *            starting and >=1 is done.
	 * @param format The format of the error
	 * @param args The arguments of the error
	 * @deprecated Use SLF4J
	 *             Logger.info(aQute.libg.slf4j.GradleLogging.LIFECYCLE)
	 *             instead.
	 */
	@Deprecated
	void progress(float progress, String format, Object... args);

	/**
	 * Dedicated message for an exception.
	 *
	 * @param t The exception
	 * @param format The format of the message
	 * @param args The arguments
	 */
	SetLocation exception(Throwable t, String format, Object... args);

	/**
	 * The provider of the reporter wants pedantic reporting, meaning every
	 * possible warning should be reported.
	 *
	 * @return if this is a pedantic reporter.
	 */
	boolean isPedantic();

}
