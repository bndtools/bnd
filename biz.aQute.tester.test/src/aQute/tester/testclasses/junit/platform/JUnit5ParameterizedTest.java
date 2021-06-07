package aQute.tester.testclasses.junit.platform;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit5ParameterizedTest {

	@ParameterizedTest(name = "{index} ==> param: ''{0}'', param2: {1}")
	@MethodSource("provideArgs")
	public void parameterizedMethod(String param, float param2) {
		if ("four".equals(param)) {
			throw new AssertionFailedError("Not expecting four, but got " + param, "four", param);
		}
	}

	public static Stream<Arguments> provideArgs() {
		return Stream.of(Arguments.of("one", 1.0f), Arguments.of("two", 2.0f), Arguments.of("three", 3.0f),
			Arguments.of("four", 4.0f), Arguments.of("five", 5.0f));
	}

	@ParameterizedTest
	@MethodSource("unknownMethod")
	public void misconfiguredMethod(String param) {}
}
