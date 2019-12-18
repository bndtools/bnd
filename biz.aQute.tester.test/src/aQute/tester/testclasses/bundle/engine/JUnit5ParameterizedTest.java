package aQute.tester.testclasses.bundle.engine;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit5ParameterizedTest {

	@ParameterizedTest
	@MethodSource("provideArgs")
	public void parameterizedMethod(String param, float param2) {
	}

	public static Stream<Arguments> provideArgs() {
		return Stream.of(Arguments.of("one", 1.0f), Arguments.of("two", 2.0f), Arguments.of("three", 3.0f),
			Arguments.of("four", 4.0f), Arguments.of("five", 5.0f));
	}
}
