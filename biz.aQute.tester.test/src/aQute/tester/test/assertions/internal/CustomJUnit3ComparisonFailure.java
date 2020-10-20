package aQute.tester.test.assertions.internal;

import junit.framework.ComparisonFailure;

// This test class is not supposed to be run directly; see readme.md for more info.
public class CustomJUnit3ComparisonFailure extends ComparisonFailure {
	private static final long serialVersionUID = 1L;

	public CustomJUnit3ComparisonFailure(String msg, String expected, String actual) {
		super(msg, expected, actual);
	}
}
