package test;

import org.junit.Test;

import junit.framework.AssertionFailedError;

public class MyTest2 {

	@Test
	public void myTest1() {}
	
	@Test
	public void myTest2() {
		org.junit.Assume.assumeFalse("Let's get outta here", true);
	}
	
	@Test
	public void myTest3() {
		throw new AssertionFailedError();
	}
	
	@Test
	public void myTest4() {
		throw new AssertionError();
	}
	
	@Test
	public void myTest6() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void myTest7() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void myTest5() {
		throw new org.opentest4j.AssertionFailedError();
	}
	
	@org.junit.jupiter.api.Test
	public void myAbortedJupterTest() {
		org.junit.jupiter.api.Assumptions.assumeFalse(true, "Abort! Abort!");
	}
}
