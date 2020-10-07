package aQute.tester.test.assertions.internal;

import org.junit.ComparisonFailure;

// This test class is not supposed to be run directly; see readme.md for more info.
public class CustomJUnit4ComparisonFailure extends ComparisonFailure {
	private static final long serialVersionUID = 1L;

	public CustomJUnit4ComparisonFailure(String msg, String expected, String actual) {
		super(msg, expected, actual);
	}
}