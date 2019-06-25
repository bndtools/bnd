package test;

import org.junit.Test;

public class ItsTest {

	@Test
	public void test1() {}
	
	@Test
	public void test2() {}
	
	@Test
	public void test3() {
		System.err.println("test3 ran");
	}
	
	@Test
	public void test4() {
		throw new AssertionError();
	}
	
	@org.junit.jupiter.api.Test
	public void test5() {
		throw new org.opentest4j.AssertionFailedError("Hi there", "expected", "actual");
	}
}
