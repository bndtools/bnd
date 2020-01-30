package aQute.junit.constants;

public interface TesterConstants {
	/**
	 * The control port used for the bi-directional control protocol for
	 * continuous testing support in the IDE. A tester may silently ignore this
	 * setting if it does not support it. If this property is set and the tester
	 * supports it, then {@link #TESTER_PORT} is ignored and instead the value
	 * of the JUnit port is sent over the control channel.
	 */
	String	TESTER_CONTROLPORT		= "tester.controlport";

	/**
	 * The port to send the JUnit information to in a format defined by Eclipse.
	 * If this property is not set, no information is sent but tests are still
	 * run.
	 */
	String	TESTER_PORT				= "tester.port";

	/**
	 * The host to send the JUnit information to in a formated defined by
	 * Eclipse. If this property is not set localhost is assumed.
	 */
	String	TESTER_HOST				= "tester.host";

	/**
	 * Fully qualified names of classes to test. If this property is null
	 * automatic mode is chosen, otherwise these classes are going to be tested
	 * and then the test ends.
	 */
	String	TESTER_NAMES			= "tester.names";

	/**
	 * A directory to put the test reports. If the directory does not exist, no
	 * reports are generated. The default is tester-dir.
	 */
	String	TESTER_DIR				= "tester.dir";

	/**
	 * In automatic mode, no {@link #TESTER_NAMES} set, continue watching the
	 * bundles and re-run a bundle's tests when it is started.
	 */
	String	TESTER_CONTINUOUS		= "tester.continuous";

	/**
	 * Trace the test framework in detail. Default is false, must be set to true
	 * or false.
	 */
	String	TESTER_TRACE			= "tester.trace";

	/**
	 * Do not run the resolved test
	 */
	String	TESTER_UNRESOLVED		= "tester.unresolved";

	/**
	 * Use a new thread to run the tests on (might be needed if a test uses the
	 * main thread for some other reason) and for backward compatibility.
	 * <p/>
	 * Note: The previous default was a separate thread so this option is mostly
	 * for backward compatibility and the unlikely case this code is used
	 * outside the bnd launcher. This approach started to fail when we allowed
	 * the embedded activators to start before all bundles were started, this
	 * created a race condition for class loading. Using the main has the
	 * guarantee that all bundles have been installed and it is kind of clean as
	 * well to use this special thread to run all tests on.
	 */
	String	TESTER_SEPARATETHREAD	= "tester.separatethread";
}
