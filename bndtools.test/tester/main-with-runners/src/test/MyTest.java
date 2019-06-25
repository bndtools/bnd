package test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import junit.framework.AssertionFailedError;

public class MyTest {

	@Test
	public void test0() {
	}
	
	@Test
	public void test1() {
		throw new AssertionFailedError("Hi there");
	}
	
	@Test
	public void test2() {
		assertThat("a").isEqualTo("x");
	}
	
	@Test
	public void test3() {
		throw new org.opentest4j.AssertionFailedError("hi", "there", "folks");
	}
	
	@Test
	public void test4() {
		throw new junit.framework.ComparisonFailure("by", "here", "oaks");
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
	public void testerer5() {
		assertThat("actual").isEqualTo("expected");
	}
}
