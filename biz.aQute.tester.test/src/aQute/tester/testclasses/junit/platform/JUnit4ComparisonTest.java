package aQute.tester.testclasses.junit.platform;

import org.junit.ComparisonFailure;
import org.junit.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit4ComparisonTest {
	@Test
	public void comparisonFailure() {
		throw new ComparisonFailure("message", "expected", "actual");
	}
}
