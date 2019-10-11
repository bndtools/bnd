package test;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestClass {

	@Test
	public void thisIsATesterer() {
		
	}

	@DisplayName("The name με τα φάνκυ χαρακτήρες")
	@Test
	public void thisFails() {
		assertThat(1).isEqualTo(2);
	}

	@Disabled("Because I wanted to")
	@Test
	public void somethingElse( ) {
		assertThat("Hi there").isEqualTo("blessings");
	}
	
	@ParameterizedTest(name = "{index} ==> param: ''{0}'', param2: {1}")
	@MethodSource("provideArgs")
	public void parameterizedMethod(String param, float param2) {
		assertThat(param)
			.isNotEqualTo("four");
	}

	public static Stream<Arguments> provideArgs() {
		return Stream.of(Arguments.of("one", 1.0f), Arguments.of("two", 2.0f), Arguments.of("three", 3.0f),
			Arguments.of("four", 4.0f), Arguments.of("five", 5.0f));
	}

	@ParameterizedTest
	@MethodSource("unknownMethod")
	public void misconfiguredMethod(String param) {
	}
}
