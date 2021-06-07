package aQute.tester.testclasses.junit.platform;

import org.junit.jupiter.api.Disabled;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit5Skipper {

	@org.junit.jupiter.api.Test
	@Disabled("with custom message")
	public void disabledTest() {}
}
