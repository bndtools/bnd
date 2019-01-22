package aQute.bnd.remote.junit;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.remote.junit.test.inject.SomeService;
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.lib.io.IO;

public class JUnitFrameworkTest {
	static Workspace		ws;
	JUnitFrameworkBuilder	builder;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// ws = Workspace.findWorkspace(IO.work);
	}

	@Before
	public void before() throws Exception {
		builder = new JUnitFrameworkBuilder();
	}

	@After
	public void after() throws Exception {
		builder.close();
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
		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework")
			.create()) {

			Foo foo = fw.newInstance(Foo.class);
			assertThat(foo.bundles).isNotNull();
			assertThat(foo.packageAdmin).isNotNull();
		}
	}

	@Test
	public void testBundleActivatorCalled() throws Exception {

		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework")
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

		try (JUnitFramework fw = builder.bndrun(new File("resources/systempackages.bndrun").getAbsolutePath())
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

		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework")
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
		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework")
			.nostart()
			.create()
			.inject(this)) {
			Closeable hide = fw.hide(String.class);
			fw.start();
			assertThat(fw.getServices(String.class)).isEmpty();

			// register via the framework since we do not hide services
			// registered
			// via the testbundle

			fw.testbundle.getBundleContext()
				.registerService(String.class, "Hello", null);

			assertThat(fw.getServices(String.class)).isEmpty();
			hide.close();
			assertThat(fw.getServices(String.class)).containsOnly("Hello");
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
		try (JUnitFramework fw = builder.bundles("org.apache.felix.log, org.apache.felix.scr")
			.runfw("org.apache.felix.framework")
			.create()) {

			Bundle start = fw.bundle()
				.addResource(Comp.class)
				.start();

			assertThat(semaphore.tryAcquire(1, 5, TimeUnit.SECONDS)).isTrue();
			assertThat(semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS)).isFalse();

			start.stop();

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
		try (JUnitFramework fw = builder.bundles()
			.runfw("org.apache.felix.framework")
			.create()
			.inject(x)) {

		}
	}

	@Test
	public void testTimeoutWithInvisibleAndFiltered() throws Exception {
		try {
			try (JUnitFramework fw = builder
				.runfw("org.apache.felix.framework")
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
			try (JUnitFramework fw = builder
				.runfw("org.apache.felix.framework")
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
			try (JUnitFramework fw = builder
				.runfw("org.apache.felix.framework")
				.create()) {

				fw.hide(SomeService.class);
				Hashtable<String, Object> properties = new Hashtable<>();

				fw.getBundleContext()
					.registerService(SomeService.class, new SomeService(), properties);

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
			assertThat(toe.getMessage()).contains("Hidden (FindHook)", "JUnitFramework[hide]");
		}
	}

	@Test
	public void testReportingUnimportedExport() throws Exception {
		try (JUnitFramework fw = builder
			.runfw("org.apache.felix.framework")
			.create()) {

			Bundle a = fw.bundle()
				.exportPackage(SomeService.class.getPackage()
					.getName())
				.header(Constants.BUNDLE_ACTIVATOR, SomeService.class.getName())
				.start();

			assertTrue(fw.check("is exporting but NOT importing package"));
		}
	}
}
