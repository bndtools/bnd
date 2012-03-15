package test;

import junit.framework.*;

public class TestCase1 extends TestCase {
	
	public void test1() {
		System.err.println("All ok");
	}

	public void test2() {
		throw new IllegalArgumentException("Don't talk to me like that!!!!!");
	}
	
	public void test3() {
		fail("I am feeling depressive");
	}
	public void test4() {
		System.err.println("All ok again");
	}
}
