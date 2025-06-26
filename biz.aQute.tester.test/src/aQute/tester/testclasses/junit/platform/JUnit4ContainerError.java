package aQute.tester.testclasses.junit.platform;

import org.junit.BeforeClass;
import org.junit.Test;

public class JUnit4ContainerError {

	@BeforeClass
	static void beforeAll() {
		throw new IllegalStateException();
	}

	@Test
	public void myTest() {
		throw new AssertionError();
	}

	@Test
	public void my2ndTest() {
		throw new AssertionError();
	}
}
