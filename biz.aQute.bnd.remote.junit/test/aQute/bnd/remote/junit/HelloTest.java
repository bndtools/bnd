package aQute.bnd.remote.junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class HelloTest {
	JUnitFrameworkBuilder	builder		= new JUnitFrameworkBuilder();
	JUnitFramework			framework = builder.runfw("org.apache.felix.framework").create();

	public static class Hello implements BundleActivator {
		static Semaphore semaphore = new Semaphore(0);
		
		@Override
		public void start(BundleContext context) throws Exception {
			System.out.println("Hello");
			semaphore.release();
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			System.out.println("Goodbye");
			semaphore.release();
		}
	}
	
	@Test
	public void testActivator() throws BundleException {
		Bundle hello = framework.bundle()
			.bundleActivator(Hello.class)
			.start();

		assertTrue(Hello.semaphore.tryAcquire());
		assertFalse(Hello.semaphore.tryAcquire());

		hello.stop();
		assertTrue(Hello.semaphore.tryAcquire());

	}
	
}
