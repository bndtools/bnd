package aQute.tester.testclasses.junit.platform;

import junit.framework.TestCase;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit3ComparisonTest extends TestCase {
	public void testComparisonFailure() {
		throw new junit.framework.ComparisonFailure("message", "expected", "actual");
	}
}
