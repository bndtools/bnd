package aQute.tester.testclasses.junit.platform;

import org.junit.jupiter.api.Disabled;

//This test class is not supposed to be run directly; see readme.md for more info.
@Disabled("with another message")
public class JUnit5ContainerSkipped {
	@org.junit.jupiter.api.Test
	public void disabledTest2() {}

	@org.junit.jupiter.api.Test
	public void disabledTest3() {}
}
