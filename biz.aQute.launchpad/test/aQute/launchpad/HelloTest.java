package aQute.launchpad;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.bnd.build.Workspace;

public class HelloTest {
	static Workspace	ws;
	LaunchpadBuilder	builder;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// ws = Workspace.findWorkspace(IO.work);
	}

	@Before
	public void before() throws Exception {
		builder = new LaunchpadBuilder();
	}

	@After
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
