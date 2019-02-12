package aQute.bnd.remote.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.remote.junit.test.inject.SomeService;
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.lib.io.IO;

public class JUnitFrameworkTest {
	static Workspace		ws;
	LauchpadBuilder	builder;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// ws = Workspace.findWorkspace(IO.work);
	}

	@Before
	public void before() throws Exception {
		builder = new LauchpadBuilder();
	}

	@After
	public void after() throws Exception {
		builder.close();
	}

	@Test
	public void testWs() {
		System.err.println("waiter");
	}

	@Test
	public void testConnection() throws Exception {
		try (RemoteWorkspace remote = RemoteWorkspaceClientFactory.create(IO.work, new RemoteWorkspaceClient() {})) {
			assertNotNull(remote);
			assertThat(remote.getLatestBundles(IO.work.getAbsolutePath(), "biz.aQute.bnd"))
				.contains(IO.getFile("../biz.aQute.bnd/generated/biz.aQute.bnd.jar")
					.getAbsolutePath());
			assertThat(remote.getProjects()).contains("biz.aQute.bnd.remote.junit");
			assertThat(remote.getBndVersion()).contains(About.CURRENT.toString());

			assertNotNull(remote.analyzeTestSetup(IO.work.getAbsolutePath()));
			assertNotNull(remote.getRun(IO.work.getAbsolutePath()));
		}
	}

	static Semaphore semaphore = new Semaphore(0);

	public static class MyActivator implements BundleActivator {

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

	@Service
	Bundle[]		bundles;

	@Service
	BundleContext	context;

	static public class Foo1 {
		@Service
		Bundle[] bundles;
	}

	static public class Foo extends Foo1 {
		@SuppressWarnings("deprecation")
		@Service
		org.osgi.service.packageadmin.PackageAdmin packageAdmin;
	}

	@Test
	public void testInjectionInherited() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {

			Foo foo = fw.newInstance(Foo.class);
			assertThat(foo.bundles).isNotNull();
			assertThat(foo.packageAdmin).isNotNull();
		}
	}

	@Test
	public void testBundleActivatorCalled() throws Exception {

		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {

			Bundle x = fw.bundle()
				.bundleActivator(MyActivator.class)
				.start();

			assertThat(semaphore.tryAcquire(1, 1, TimeUnit.SECONDS)).isTrue();
			assertThat(semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS)).isFalse();

			x.stop();

			assertThat(semaphore.tryAcquire(1, 1, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	public void testRunSystemPackages() throws Exception {
		// Exports blabar

		try (Launchpad fw = builder.bndrun(new File("testresources/systempackages.bndrun").getAbsolutePath())
			.create()
			.inject(this)) {

			// should throw an exception
			// if it cannot resolve

			fw.bundle()
				.importPackage("blabar")
				.start();

		}
	}

	@Test
	public void testConfiguration() throws Exception {

		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()
			.inject(this)) {

			Bundle cfg = fw.bundle()
				.addConfiguration("[{}]")
				.start();

			URL entry = cfg.getEntry("configuration/configuration.json");
			assertThat(entry).isNotNull();

			String txt = IO.collect(entry.openStream());
			assertThat(txt).isEqualTo("[{}]");
		}
	}

	/**
	 * Hide a service
	 */

	@Test
	public void testHiding() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.nostart()
			.create()
			.inject(this)) {

			Closeable hide = fw.hide(String.class);
			fw.start();

			boolean isHidden = fw.getServices(String.class)
				.isEmpty();
			assertThat(isHidden).isTrue();

			fw.framework.getBundleContext()
				.registerService(String.class, "fw", null);

			isHidden = fw.getServices(String.class)
				.isEmpty();
			assertThat(isHidden).isTrue();

			ServiceRegistration<String> visibleToAllViaTestbundle = fw.register(String.class, "Hello");

			assertThat(fw.getServices(String.class)).containsOnly("Hello");

			visibleToAllViaTestbundle.unregister();

			isHidden = fw.getServices(String.class)
				.isEmpty();
			assertThat(isHidden).isTrue();

			hide.close();
			assertThat(fw.getServices(String.class)).containsOnly("fw");
		}

	}

	/**
	 * Test a built in component
	 */

	@Component(immediate = true, service = Comp.class)
	public static class Comp {

		@Activate
		void activate() {
			semaphore.release();
			System.out.println("Activate");
		}

		@Deactivate
		void deactivate() {
			semaphore.release();
			System.out.println("Deactivate");
		}
	}

	@Test
	public void testComponent() throws Exception {
		try (Launchpad fw = builder.bundles("org.apache.felix.log, org.apache.felix.scr")
			.runfw("org.apache.felix.framework")
			.create()) {

			Closeable comp = fw.addComponent(Comp.class);

			assertThat(semaphore.tryAcquire(1, 5, TimeUnit.SECONDS)).isTrue();
			assertThat(semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS)).isFalse();

			assertThat(fw.getService(Comp.class)).containsInstanceOf(Comp.class);

			comp.close();

			assertThat(fw.getService(Comp.class)).isEmpty();

			assertThat(semaphore.tryAcquire(1, 1, TimeUnit.SECONDS)).isTrue();

		}

	}

	@Test(expected = TimeoutException.class)
	public void testTimeout() throws Exception {
		class X {
			@Service(timeout = 500)
			String s;
		}
		X x = new X();
		try (Launchpad fw = builder.bundles()
			.runfw("org.apache.felix.framework")
			.create()
			.inject(x)) {

		}
	}

	@Test
	public void testTimeoutWithInvisibleAndFiltered() throws Exception {
		try {
			try (Launchpad fw = builder.runfw("org.apache.felix.framework")
				.create()) {

				Hashtable<String, Object> properties = new Hashtable<>();
				properties.put("foo", "hello");

				fw.getBundleContext()
					.registerService(SomeService.class, new SomeService(), properties);
				Bundle a = fw.bundle()
					.exportPackage(SomeService.class.getPackage()
						.getName())
					.header(Constants.BUNDLE_ACTIVATOR, SomeService.class.getName())
					.start();
				Bundle b = fw.bundle()
					.exportPackage(SomeService.class.getPackage()
						.getName())
					.header(Constants.BUNDLE_ACTIVATOR, SomeService.class.getName())
					.start();

				class X {
					@Service(minimum = 2, timeout = 100, target = "(!(foo=hello))")
					List<aQute.bnd.remote.junit.test.inject.SomeService> l;
				}
				X x = new X();

				fw.inject(x);
				SomeService afoo = x.l.get(0);
				SomeService bfoo = x.l.get(1);
			}
			fail();
		} catch (TimeoutException toe) {
			System.out.println(toe);
			assertThat(toe.getMessage()).contains("Invisible reference", "Reference not matched by the target filter");
		}
	}

	@Test
	public void testTimeoutWithPrivatePackage() throws Exception {
		try {
			try (Launchpad fw = builder.runfw("org.apache.felix.framework")
				.create()) {

				Bundle a = fw.bundle()
					.privatePackage(SomeService.class.getPackage()
						.getName())
					.header(Constants.BUNDLE_ACTIVATOR, SomeService.class.getName())
					.start();

				class X {
					@Service(minimum = 2, timeout = 100)
					List<aQute.bnd.remote.junit.test.inject.SomeService> l;
				}
				X x = new X();

				fw.inject(x);
				SomeService afoo = x.l.get(0);
				SomeService bfoo = x.l.get(1);
			}
			fail();
		} catch (TimeoutException toe) {
			System.out.println(toe);
			assertThat(toe.getMessage()).contains("Invisible reference", "PRIVATE");
		}
	}

	@Test
	public void testReportingHiddenService() throws Exception {
		try {
			try (Launchpad fw = builder.runfw("org.apache.felix.framework")
				.create()) {

				fw.framework.getBundleContext()
					.registerService(SomeService.class, new SomeService(), null);

				fw.hide(SomeService.class);
				fw.getBundleContext()
					.registerService(SomeService.class, new SomeService(), null);

				class X {
					@Service(minimum = 2, timeout = 100)
					List<aQute.bnd.remote.junit.test.inject.SomeService> l;
				}
				X x = new X();
				fw.inject(x);
			}
			fail();
		} catch (TimeoutException toe) {
			System.out.println(toe);
			assertThat(toe.getMessage()).contains("Hidden (FindHook)", "JUnitFramework[hide]");
		}
	}

	@Test
	public void testReportingUnimportedExport() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {

			Bundle a = fw.bundle()
				.exportPackage(SomeService.class.getPackage()
					.getName())
				.header(Constants.BUNDLE_ACTIVATOR, SomeService.class.getName())
				.start();

			assertTrue(fw.check("is exporting but NOT importing package"));
		}
	}

	@Test
	public void testGetService() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {

			assertFalse(fw.getService(String.class)
				.isPresent());
		}
	}

	@Test
	public void testRegisterService() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {
			fw.register(String.class, "Hello", "a", 1, "b", 2, "c", new int[] {
				3, 4, 5
			});

			assertTrue(fw.getService(String.class, "(&(a=1)(b=2)(c=3))")
				.isPresent());
		}

	}
	// wait a bit until this is supported by gradle and Eclipse
	// @Test(expected = IllegalArgumentException.class)
	// public void testMissingBundle() throws Exception {
	// try (JUnitFramework fw = builder.bndrun("missing.bndrun")
	// .create()) {}
	// }

	@Component(service = ExternalRefComp.class)
	public static class ExternalRefComp {

		@Reference
		Processor p;
	}

	@Test
	public void componentWithExternalReferences() throws Exception {
		try (Launchpad fw = builder.bundles("org.apache.felix.log, org.apache.felix.scr")
			.runfw("org.apache.felix.framework")
			.create()) {
			fw.addComponent(ExternalRefComp.class);
		}
	}
}
