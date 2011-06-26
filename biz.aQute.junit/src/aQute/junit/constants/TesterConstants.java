package aQute.junit.constants;

public interface TesterConstants {
	/**
	 * The port to send the JUnit information to in a format defined by Eclipse.
	 * If this property is not set, no information is send but tests are still
	 * run.
	 */
	String	TESTER_PORT			= "tester.port";

	/**
	 * The host to send the JUnit information to in a formated defined by
	 * Eclipse. If this property is not set localhost is assumed.
	 */
	String	TESTER_HOST			= "tester.host";

	/**
	 * Fully qualified names of classes to test. If this property is null
	 * automatic mode is chosen, otherwise these classes are going to be tested
	 * and then the test ends.
	 */
	String	TESTER_NAMES		= "tester.names";

	/**
	 * A directory to put the test reports. If the directory does not exist, no
	 * reports are generated. The default is tester-dir.
	 */
	String	TESTER_DIR			= "tester.dir";

	/**
	 * In automatic mode, no {@link #TESTER_NAMES} set, continue watching the
	 * bundles and re-run a bundle's tests when it is started.
	 */
	String	TESTER_CONTINUOUS	= "tester.continuous";

	/**
	 * Trace the test framework in detail. Default is false, must be set to true
	 * or false.
	 */
	String	TESTER_TRACE		= "tester.trace";

}
