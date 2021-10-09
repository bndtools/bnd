package aQute.launchpad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.bnd.build.Workspace;

public class HelloTest {
	static Workspace	ws;
	LaunchpadBuilder	builder;

	@BeforeEach
	public void before() throws Exception {
		builder = new LaunchpadBuilder();
	}

	@AfterEach
	public void after() throws Exception {
		builder.close();
	}

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
	public void testActivator() throws Exception {
		try (Launchpad framework = builder.runfw("org.apache.felix.framework")
			.create()) {
			Bundle hello = framework.bundle()
				.bundleActivator(Hello.class)
				.start();

			assertTrue(Hello.semaphore.tryAcquire());
			assertFalse(Hello.semaphore.tryAcquire());

			hello.stop();
			assertTrue(Hello.semaphore.tryAcquire());
		}
	}

}
