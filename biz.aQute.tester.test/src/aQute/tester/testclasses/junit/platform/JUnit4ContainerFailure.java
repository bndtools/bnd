package aQute.tester.testclasses.junit.platform;

import org.junit.BeforeClass;
import org.junit.Test;

public class JUnit4ContainerFailure {

	@BeforeClass
	void beforeAll() {
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
