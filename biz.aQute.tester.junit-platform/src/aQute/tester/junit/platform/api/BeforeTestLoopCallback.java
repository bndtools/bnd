package aQute.tester.junit.platform.api;

public interface BeforeTestLoopCallback {
	/**
	 * Callback that is invoked by the Bnd JUnit Platform tester before it
	 * starts the testing loop. Calls to this callback are synchronous.
	 */
	void beforeTestLoop();
}
