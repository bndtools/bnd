package aQute.tester.testclasses.tester;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

// This test class is not supposed to be run directly; see readme.md for more info.
public class JUnit3NonStaticFieldBC extends TestCase {
	public BundleContext							context;
	public static AtomicReference<BundleContext>	actualBundleContext	= new AtomicReference<>();

	public void testSomething() {
		actualBundleContext.set(context);
	}
}
