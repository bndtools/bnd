package aQute.tester.testclasses.junit.platform;

import org.junit.Ignore;
import org.junit.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit4Skipper {
	@Test
	@Ignore("This is a test")
	public void disabledTest() {}

	@Test
	public void enabledTest() {}
}
