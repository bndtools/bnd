package test;

import junit.framework.TestCase;

public class Test1 extends TestCase {

	public void testSuccess() {
		System.out.println("testSuccess!");
		assertEquals(true, true);
	}	
	public void testFail() {
		System.out.println("testSuccess!");
		assertEquals(true, true);
	}
}
