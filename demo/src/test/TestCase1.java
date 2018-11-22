package test;

import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

public class TestCase1 extends TestCase {

	@SuppressWarnings("unused")
	private volatile BundleContext context;

	/**
	 * This method is called by the JUnit runner for OSGi, and gives us a Bundle
	 * Context.
	 */
	public void setBundleContext(BundleContext context) {
		this.context = context;
	}

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
