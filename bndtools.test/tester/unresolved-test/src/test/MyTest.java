package test;

import org.junit.Test;

import junit.framework.AssertionFailedError;

public class MyTest {

	@Test
	public void test1() {}
	
	@Test
	public void test2() {}
	
	@Test
	public void test3() {
		throw new AssertionFailedError();
	}
	
	@Test
	public void test4() {
		throw new AssertionError();
	}
	
	@Test
	public void test6() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void test7() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void test5() {
		throw new org.opentest4j.AssertionFailedError();
	}
}
