package xtest;

import junit.framework.*;

public class ArchetypicalTest extends TestCase  {
	Spec s = new Spec() {

		public void bar() {
		}

		public void foo() {
		}
		
	};
	public void testSuccess() {
		System.out.println("I am a success!");
	}
	
	public void testFailure() {
		System.out.println("I am a failure!");
		fail();
	}
	
	public void testError() {
		System.out.println("I am an error!");
		throw new IllegalArgumentException("I am an error");
	}
	
	public void testCoverage() {
		s.foo();
	}
}
