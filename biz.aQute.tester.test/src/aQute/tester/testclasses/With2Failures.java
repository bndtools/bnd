package aQute.tester.testclasses;

import org.junit.Test;

import aQute.tester.test.assertions.CustomAssertionError;

// This test class is not supposed to be run directly; see readme.md for more info.
public class With2Failures {
	@Test
	public void test1() {
		throw new AssertionError();
	}

	@Test
	public void test2() {}

	@Test
	public void test3() {
		throw new CustomAssertionError();
	}
}
