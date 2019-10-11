package aQute.tester.testclasses;

import org.junit.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class With1Error1Failure {
	@Test
	public void test1() {
		throw new RuntimeException();
	}

	@Test
	public void test2() {}

	@Test
	public void test3() {
		throw new AssertionError();
	}
}
