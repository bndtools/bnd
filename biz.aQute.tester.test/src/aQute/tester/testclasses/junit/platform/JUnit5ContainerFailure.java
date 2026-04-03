package aQute.tester.testclasses.junit.platform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JUnit5ContainerFailure {

	@BeforeAll
	static void beforeAll() {
		throw new AssertionError();
	}

	@Test
	void myTest() {
		throw new AssertionError();
	}

	@Test
	void my2ndTest() {
		throw new AssertionError();
	}
}
