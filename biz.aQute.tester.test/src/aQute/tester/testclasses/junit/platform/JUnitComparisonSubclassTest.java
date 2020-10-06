package aQute.tester.testclasses.junit.platform;

import java.util.ArrayList;
import java.util.List;

import aQute.tester.test.assertions.internal.CustomAssertionFailedError;
import aQute.tester.test.assertions.internal.CustomJUnit3ComparisonFailure;
import aQute.tester.test.assertions.internal.CustomJUnit4ComparisonFailure;
import aQute.tester.test.assertions.internal.CustomMultipleFailuresError;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnitComparisonSubclassTest {
	@org.junit.jupiter.api.Test
	public void multipleComparisonFailure() {
		List<Throwable> bucket2 = new ArrayList<>();
		bucket2.add(new CustomJUnit4ComparisonFailure("ju41", "expected3.1", "actual3.1"));
		bucket2.add(new RuntimeException("rt1"));
		bucket2.add(new CustomJUnit3ComparisonFailure("ju31", "expected3.2", "actual3.2"));
		bucket2.add(new CustomAssertionFailedError("afe1", "expected3.4", "actual3.4"));

		List<Throwable> bucket1 = new ArrayList<>();
		bucket1.add(new CustomJUnit4ComparisonFailure("ju42", "expected1", "actual1"));
		bucket1.add(new RuntimeException("rt2"));
		bucket1.add(new CustomJUnit3ComparisonFailure("ju32", "expected2", "actual2"));
		bucket1.add(new CustomMultipleFailuresError("mfe2", bucket2));
		bucket1.add(new CustomAssertionFailedError("afe2", "expected4", "actual4"));

		throw new CustomMultipleFailuresError("mfe1", bucket1);
	}
}
