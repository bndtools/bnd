package test;

import junit.framework.TestCase;

public class EcjTest extends TestCase {

	public static class A {
		public A foo() {
			System.out.println("In A");
			return null;
		}
		// Object foo() ...
	}

	public static class B extends A {
		@Override
		public B foo() {
			System.out.println("In B");
			return null;
		}
		// A foo() ...
	}

	public static class C extends B {
		@Override
		public C foo() {
			System.out.println("In C");
			return null;
		}
		// B foo() ...
	}

	public void testSimple() {
		A c = new C();
		c.foo(); // 9: invokevirtual #18; //Method
					// test/EcjTest$A.foo:()Ltest/EcjTest$A;
		// but prints in C
	}

}
