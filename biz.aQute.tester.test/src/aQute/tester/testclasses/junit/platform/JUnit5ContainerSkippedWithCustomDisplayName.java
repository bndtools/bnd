package aQute.tester.testclasses.junit.platform;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

//This test class is not supposed to be run directly; see readme.md for more info.
@DisplayName("Skipper Class")
@Disabled("with a third message")
public class JUnit5ContainerSkippedWithCustomDisplayName {
	@org.junit.jupiter.api.Test
	public void disabledTest2() {}

	@org.junit.jupiter.api.Test
	public void disabledTest3() {}
}