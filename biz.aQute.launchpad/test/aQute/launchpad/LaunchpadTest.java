package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.launchpad.test.inject.SomeService;
import aQute.lib.io.IO;
import aQute.libg.parameters.ParameterMap;

public class LaunchpadTest {
	@Rule
	public final JUnitSoftAssertions	softly	= new JUnitSoftAssertions();

	LaunchpadBuilder					builder;
	File								tmp;

	@Before
	public void before() throws Exception {
		builder = new LaunchpadBuilder();

	}

	@After
	public void after() throws Exception {
		builder.close();
	}

	@Test
	public void testExportExcludes() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.excludeExport("org.slf4j*,aQute.lib*")
			.create()) {
			Set<String> p = new ParameterMap(fw.getBundleContext()
				.getProperty("org.osgi.framework.system.packages.extra")).keySet();
			assertThat(p).doesNotContain("aQute.lib.io", "org.slf4j");
		}
	}

	@Test
	public void testRunsystemPackages() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.bndrun("runsystempackages.bndrun")
			.create()) {
			Set<String> p = new ParameterMap(fw.getBundleContext()
				.getProperty("org.osgi.framework.system.packages.extra")).keySet();
			assertThat(p).contains("sun.misc");
		}
	}

	@Test
	public void testConnection() throws Exception {
		try (RemoteWorkspace remote = RemoteWorkspaceClientFactory.create(IO.work, new RemoteWorkspaceClient() {})) {
			assertNotNull(remote);
			assertThat(remote.getLatestBundles(IO.work.getAbsolutePath(), "biz.aQute.bnd"))
				.contains(IO.getFile("../biz.aQute.bnd/generated/biz.aQute.bnd.jar")
					.getAbsolutePath());
			assertThat(remote.getProjects()).contains("biz.aQute.launchpad");

			assertNotNull(remote.analyzeTestSetup(IO.work.getAbsolutePath()));
			assertNotNull(remote.getRun(IO.work.getAbsolutePath()));
		}
	}

	static final CountDownLatch	activatedMyActivator	= new CountDownLatch(1);
	static final CountDownLatch	deactivatedMyActivator	= new CountDownLatch(1);

	public static class MyActivator implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			System.out.println("Hello");
			activatedMyActivator.countDown();
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			System.out.println("Goodbye");
			deactivatedMyActivator.countDown();
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
			.debug()
			.create()) {

			Bundle x = fw.bundle()
				.bundleActivator(MyActivator.class)
				.start();

			assertThat(activatedMyActivator.await(1, TimeUnit.SECONDS)).isTrue();
			assertThat(deactivatedMyActivator.await(500, TimeUnit.MILLISECONDS)).isFalse();

			x.stop();

			assertThat(deactivatedMyActivator.await(1, TimeUnit.SECONDS)).isTrue();
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

			Bundle capability = fw.bundle()
				.provideCapability("osgi.extender")
				.attribute("osgi.extender", "osgi.configurator")
				.version("1.0.0")
				.start();

			Bundle cfg = fw.bundle()
				.addConfiguration("[{}]")
				.start();

			URL entry = cfg.getEntry("OSGI-INF/configurator/configuration.json");
			assertThat(entry).isNotNull();

			String txt = IO.collect(entry.openStream());
			assertThat(txt).isEqualTo("[{}]");
		}
	}

	/**
	 * Test a built in component
	 */

	static final CountDownLatch	activatedComp	= new CountDownLatch(1);
	static final CountDownLatch	deactivatedComp	= new CountDownLatch(1);

	@Component(immediate = true, service = Comp.class)
	public static class Comp {

		@Activate
		void activate() {
			activatedComp.countDown();
			System.out.println("Activate");
		}

		@Deactivate
		void deactivate() {
			deactivatedComp.countDown();
			System.out.println("Deactivate");
		}
	}

	@Test
	public void testComponent() throws Exception {
		try (Launchpad fw = builder.bundles("org.apache.felix.log, org.apache.felix.scr")
			.runfw("org.apache.felix.framework")
			.create()) {

			Bundle comp = fw.component(Comp.class);

			assertThat(activatedComp.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(deactivatedComp.await(200, TimeUnit.MILLISECONDS)).isFalse();

			assertThat(fw.getService(Comp.class)).containsInstanceOf(Comp.class);

			comp.uninstall();

			assertThat(fw.getService(Comp.class)).isEmpty();

			assertThat(deactivatedComp.await(1, TimeUnit.SECONDS)).isTrue();

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
					List<aQute.launchpad.test.inject.SomeService> l;
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
					List<aQute.launchpad.test.inject.SomeService> l;
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
					List<aQute.launchpad.test.inject.SomeService> l;
				}
				X x = new X();
				fw.inject(x);
			}
			fail();
		} catch (TimeoutException toe) {
			System.out.println(toe);
			assertThat(toe.getMessage()).contains("Hidden (FindHook)", "Launchpad[hide]");
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

			assertThat(fw.getService(String.class)).isEmpty();
		}
	}

	@Test
	public void testRegisterService() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {
			fw.register(String.class, "Hello", "a", 1, "b", 2, "c", new int[] {
				3, 4, 5
			});

			assertThat(fw.getService(String.class, "(&(a=1)(b=2)(c=3))")).isNotEmpty();
		}

	}
	// wait a bit until this is supported by gradle and Eclipse
	// @Test(expected = IllegalArgumentException.class)
	// public void testMissingBundle() throws Exception {
	// try (Launchpad lp = builder.bndrun("missing.bndrun")
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
			fw.component(ExternalRefComp.class);
		}
	}

	@Test
	public void testAutoname() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create()) {
			assertThat(fw.getName()).isEqualTo("testAutoname");
			assertThat(fw.getClassName()).isEqualTo(LaunchpadTest.class.getName());
		}
	}

	@Test
	public void testSetName() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create("foo")) {
			assertThat(fw.getName()).isEqualTo("foo");
			assertThat(fw.getClassName()).isEqualTo(LaunchpadTest.class.getName());
		}
	}

	@Test
	public void testSetNameAndClassName() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.create("foo", "bar")) {
			assertThat(fw.getName()).isEqualTo("foo");
			assertThat(fw.getClassName()).isEqualTo("bar");
		}
	}

	@Test
	public void testLaunchpadStressByCreatingLotsOfFrameworksInDifferentThreads() throws Exception {
		List<Launchpad> l = new CopyOnWriteArrayList<>();
		List<Throwable> e = new CopyOnWriteArrayList<>();
		Random r = new Random();
		Semaphore semaphore = new Semaphore(0);
		builder.bundles("org.apache.felix.log, org.apache.felix.scr")
			.runfw("org.apache.felix.framework");

		int n = 20;
		for (int i = 0; i < n; i++) {

			Launchpad fw = builder.create("foo", "bar");
			l.add(fw);

			Processor.getExecutor()
				.execute(() -> {
					try {
						Bundle bundle = fw.component(ExternalRefComp.class);
						int sleep = Math.abs(r.nextInt() % 100) + 30;
						System.out.println(
							fw.runspec.properties.get(org.osgi.framework.Constants.FRAMEWORK_STORAGE) + " " + sleep);
						Thread.sleep(sleep);
						softly.assertThat(fw.getService(ExternalRefComp.class))
							.isNotNull();
						bundle.stop();
					} catch (Throwable ee) {
						softly.fail(ee.toString(), ee);
					} finally {
						semaphore.release();
					}
				});
		}
		System.out.println("Waiting for the threads to finish");
		boolean success = semaphore.tryAcquire(n, 60, TimeUnit.SECONDS);
		softly.assertThat(success)
			.isTrue();
		System.out.println("Closing " + n + " frameworks");
		l.forEach(IO::close);
	}

	public static class TestClass implements Supplier<Bundle> {
		@Override
		public Bundle get() {
			return FrameworkUtil.getBundle(this.getClass());
		}
	}

	public static class TestClass2 implements Supplier<Object[]> {

		private final Object[] args;

		public TestClass2(Object[]... args) {
			this.args = args;
		}

		@Override
		public Object[] get() {
			return args;
		}
	}

	@Test
	public void instantiateInFramework_instantiatesTheClass_insideTheFramework() throws Exception {
		try (Launchpad lp = builder.runfw("org.apache.felix.framework")
			.create()) {
			Supplier<Bundle> b = lp.instantiateInFramework(TestClass.class);
			assertThat(b).as("inside")
				.isNotNull();
			assertThat(b).isNotInstanceOf(TestClass.class);
			assertThat(b.getClass()
				.getName()).isEqualTo(TestClass.class.getName());
			// Indicates that it actually ran inside the bundle.
			assertThat(b.get()).isNotNull();
		}
	}

	@Test
	public void instantiateInFramework_withNoDefaultConstructor_throwsException() throws Exception {
		try (Launchpad lp = builder.runfw("org.apache.felix.framework")
			.create()) {
			assertThatThrownBy(() -> lp.instantiateInFramework(TestClass2.class))
				.isInstanceOf(NoSuchMethodException.class);
		}
	}
}
