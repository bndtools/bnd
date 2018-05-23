package test.simple;

import junit.framework.TestCase;

public class FailingTest extends TestCase {

	public void testFails() {
		Simple s = new Simple();
		String name = s.getName();
		System.out.println(name);
		assertEquals("ERROR!", name);
	}
}
