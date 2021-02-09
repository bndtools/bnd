package aQute.tester.test2.junit5;

import org.junit.jupiter.api.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit5Test {

	@Test
	public void somethingElse() {}

	@Test
	public void somethingElseAgain() throws Exception {
		throw new Exception();
	}
}
