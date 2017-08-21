package aQute.bnd.junit;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.junit.JUnitFramework.BundleBuilder;
import aQute.bnd.osgi.EmbeddedResource;

@SuppressWarnings("deprecation")
public class JUnitFrameworkTest extends Assert {
	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
	}
	static JUnitFramework framework = new JUnitFramework();

	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("close " + framework);
		framework.close();
	}

	@Test
	public void testBasicFramework() throws Exception {

		assertNotNull("The framework has a bundle context", framework.context);

		assertEquals("This context is from the framework itself", 0L, framework.context.getBundle().getBundleId());

		assertTrue("There are only the basic services from the framework",
				3 <= framework.context.getServiceReferences((String) null, null).length);
		assertNotNull("Package Admin", framework.getService(PackageAdmin.class));
		assertNotNull("StartLevel Service Admin", framework.getService(StartLevel.class));
	}

	public static class Activator implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			assertNotNull(context);
			context.registerService(String.class, "Hello World", null);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			fail();
		}

	}

	@Test
	public void testInstallBundle() throws Exception {
		int length = framework.context.getBundles().length;
		BundleBuilder bundle = framework.bundle();

		bundle.setBundleActivator(Activator.class.getName());
		bundle.setPrivatePackage(Activator.class.getPackage().getName());

		Bundle b = bundle.install();
		assertEquals("Not started yet", Bundle.INSTALLED, b.getState());
		assertNull("No services yet", b.getRegisteredServices());

		b.start();
		assertEquals("Now active", Bundle.ACTIVE, b.getState());
		assertNotNull("Registered service", b.getRegisteredServices());

		String s = framework.getService(String.class);
		assertEquals("Hello World", s);

		try {
			b.stop();
			fail();
		} catch (BundleException e) {
			assertTrue(e.getCause() instanceof AssertionError);
		}
		assertEquals("Now resolved", Bundle.RESOLVED, b.getState());
		assertNull("No services anymore", b.getRegisteredServices());

		b.uninstall();

		assertEquals("Only the framework left", length, framework.context.getBundles().length);
	}

	static class MyClass {
		private BundleTracker<Bundle> tracker;

		MyClass(BundleContext context) {
			tracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE, null) {
				@Override
				public Bundle addingBundle(Bundle bundle, BundleEvent event) {
					URL url = bundle.getEntry("/foo");
					if (url != null) {
						System.out.println("I am doing something very domain specific " + url);
						return bundle;
					}
					return null;
				}

				@Override
				public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
					System.out.println("Done doing something very domain specific ");
				}
			};
			tracker.open();
		}
	}

	@Test
	public void demoResourceInBundle() throws Exception {
		BundleBuilder bundle = framework.bundle();

		bundle.addResource("foo", new EmbeddedResource("Hello World".getBytes(), 0L));
		Bundle b = bundle.install();
		b.start();
		
		assertNotNull(b.getResource("foo"));
	}

	@Test(expected=BundleException.class)
	public void createCrappyBundle() throws Exception {
		BundleBuilder builder = framework.bundle();
		builder.setBundleActivator("asdasdasd");
		Bundle b = builder.install();
		assertFalse(b.getBundleId() == 0);
		b.start();

	}

	@Test
	public void demoAddBundle() throws Exception {
		Bundle bundle = framework.addBundle("org.apache.felix.configadmin").get(0);
		assertNotNull(bundle);
		assertEquals("Expect log & reader service", 2, bundle.getRegisteredServices().length);
		bundle.start();
		assertNotNull(framework.getService(ConfigurationAdmin.class));

	}

}
