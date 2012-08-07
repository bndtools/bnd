package test;

import junit.framework.*;

public class TestCase1 extends TestCase {

	public static void test1() {
		System.err.println("All ok");
	}

	public static void test2() {
		throw new IllegalArgumentException("Don't talk to me like that!!!!!");
	}

	public static void test3() {
		fail("I am feeling depressive");
	}

	public static void test4() {
		System.err.println("All ok again");
	}
}
