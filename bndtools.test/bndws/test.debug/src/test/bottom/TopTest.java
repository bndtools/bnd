package test.bottom;

import test.debug.dep.Foo;
import junit.framework.TestCase;

public class TopTest extends TestCase {

	public void testX() {
		new Foo();
		System.out.println("hello");
	}
}
