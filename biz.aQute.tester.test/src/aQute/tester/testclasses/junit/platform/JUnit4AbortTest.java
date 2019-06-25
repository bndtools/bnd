package aQute.tester.testclasses.junit.platform;

import org.junit.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit4AbortTest {
	@Test
	public void abortedTest() {
		org.junit.Assume.assumeTrue("Let's get outta here", false);
		throw new AssertionError();
	}

	@Test
	public void completedTest() {}
}
