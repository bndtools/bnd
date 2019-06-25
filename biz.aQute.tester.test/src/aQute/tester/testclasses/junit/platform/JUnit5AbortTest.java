package aQute.tester.testclasses.junit.platform;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit5AbortTest {

	@org.junit.jupiter.api.Test
	public void abortedTest() {
		org.junit.jupiter.api.Assumptions.assumeFalse(true, "I just can't go on");
		throw new AssertionError();
	}
}
