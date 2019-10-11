package aQute.tester.testclasses.junit.platform;

import java.util.ArrayList;
import java.util.List;

import org.junit.ComparisonFailure;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnitMixedComparisonTest {
	@org.junit.jupiter.api.Test
	public void multipleComparisonFailure() {
		List<Throwable> bucket2 = new ArrayList<>();
		bucket2.add(new ComparisonFailure("message", "expected3.1", "actual3.1"));
		bucket2.add(new RuntimeException());
		bucket2.add(new junit.framework.ComparisonFailure("message", "expected3.2", "actual3.2"));
		bucket2.add(new AssertionFailedError("message", "expected3.4", "actual3.4"));

		List<Throwable> bucket1 = new ArrayList<>();
		bucket1.add(new ComparisonFailure("message", "expected1", "actual1"));
		bucket1.add(new RuntimeException());
		bucket1.add(new junit.framework.ComparisonFailure("message", "expected2", "actual2"));
		bucket1.add(new MultipleFailuresError("message", bucket2));
		bucket1.add(new AssertionFailedError("message", "expected4", "actual4"));

		throw new MultipleFailuresError("message", bucket1);
	}
}
