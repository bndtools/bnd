package aQute.tester.test.assertions.internal;

import org.opentest4j.AssertionFailedError;

// This test class is not supposed to be run directly; see readme.md for more info.
public class CustomAssertionFailedError extends AssertionFailedError {
	private static final long serialVersionUID = 1L;

	public CustomAssertionFailedError(String msg, String expected, String actual) {
		super(msg, expected, actual);
	}
}