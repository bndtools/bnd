package aQute.bnd.remote.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.Closeable;
import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.lib.io.IO;

public class JUnitFrameworkTest {
	static Workspace		ws;
	JUnitFrameworkBuilder	builder;

	@BeforeClass
	public static void beforeClass() throws Exception {
		ws = Workspace.findWorkspace(IO.work);
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
		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework;version='[3,4)'")
			.create()) {

			Foo foo = fw.newInstance(Foo.class);
			assertThat(foo.bundles).isNotNull();
			assertThat(foo.packageAdmin).isNotNull();
		}
	}

	@Test
	public void testBundleActivatorCalled() throws Exception {

		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework;version='[3,4)'")
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

		try (JUnitFramework fw = builder.bndrun("resources/systempackages.bndrun")
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

		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework;version='[3,4)'")
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
		try (JUnitFramework fw = builder.runfw("org.apache.felix.framework;version='[3,4)'")
			.nostart()
			.create()
			.inject(this)) {
			Closeable hide = fw.hide(String.class);
			fw.start();
			assertThat(fw.getServices(String.class)).isEmpty();

			// register via the framework since we do not hide services
			// registered
			// via the testbundle

			fw.getFramework()
			.getBundleContext()
			.registerService(String.class.getName(), "Hello", null);

			assertThat(fw.getServices(String.class)).isEmpty();
			hide.close();
			assertThat(fw.getServices(String.class)).containsOnly("Hello");
		}

	}

	/**
	 * Test a built-in component
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
		try (JUnitFramework fw = builder
			.bundles("org.apache.felix.scr;version='[1.6,2.0)'")
			.runfw("org.apache.felix.framework;version='[3,4)'")
			.create()) {

			Bundle bundle = fw.bundle()
				.addResource(Comp.class)
				.start();

			assertThat(semaphore.tryAcquire(1, 5, TimeUnit.SECONDS)).isTrue();
			assertThat(semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS)).isFalse();

			bundle.stop();

			assertThat(semaphore.tryAcquire(1, 1, TimeUnit.SECONDS)).isTrue();

		}

	}
}
