package aQute.xlaunchpad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;

public class Manual {
	LaunchpadBuilder	builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework");

	@Service
	BundleContext		context;

	@Test
	public void quickStart() throws Exception {
		try (Launchpad launchpad = builder.create()
			.inject(this)) {
			assertNotNull(context);
		}
	}

	public static class Activator implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			System.out.println("Hello World");
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			System.out.println("Goodbye World");
		}

	}

	@Test
	public void activator() throws Exception {
		try (Launchpad launchpad = builder.create()
			.inject(this)) {

			Bundle b = launchpad.bundle()
				.bundleActivator(Activator.class)
				.start();

			assertThat(launchpad.isActive(b)).isTrue();
		}
	}

	interface Foo {

		void bar();
	}

	@Test
	public void services() throws Exception {

		try (Launchpad launchpad = builder.create()
			.debug()
			.report()) {

			ServiceRegistration<Foo> register = launchpad.register(Foo.class, () -> {
				//
			});
			Optional<Foo> s = launchpad.waitForService(Foo.class, 100);
			assertThat(s.isPresent()).isTrue();

			register.unregister();

			s = launchpad.waitForService(Foo.class, 100);
			assertThat(s.isPresent()).isFalse();

			Optional<Foo> service = launchpad.getService(Foo.class);
			assertThat(service.isPresent()).isFalse();

		}
	}

	@Test
	public void inject() throws Exception {

		try (Launchpad launchpad = builder.create()) {

			ServiceRegistration<Foo> register = launchpad.register(Foo.class, () -> {
				//
			});

			class I {
				@Service
				Foo				foo;
				@Service
				Bundle			bundles[];
				@SuppressWarnings("hiding")
				@Service
				BundleContext	context;
			}
			I inject = new I();
			launchpad.inject(inject);
			assertThat(inject.bundles).isNotEmpty();

			register.unregister();
		}
	}

	interface Bar {
		void bar();
	}

	@Component
	public static class C {
		@Reference
		Bar bar;

		@Activate
		void activate() {
			bar.bar();
		}
	}

	@Test
	public void component() throws Exception {
		try (Launchpad launchpad = builder.bundles("org.apache.felix.log")
			.bundles("org.apache.felix.scr")
			.bundles("org.apache.felix.configadmin")
			.create()) {

			launchpad.component(C.class);
			AtomicBoolean called = new AtomicBoolean(false);
			launchpad.register(Bar.class, () -> called.set(true));
			assertThat(called.get()).isTrue();
		}
	}

	@Test
	public void debug() throws Exception {
		try (Launchpad launchpad = builder.bundles("org.apache.felix.log")
			.bundles("org.apache.felix.scr")
			.bundles("org.apache.felix.configadmin")
			.debug()
			.create()) {
			//
		}

	}

	@Test
	public void gogo() throws Exception {
		// try (Launchpad launchpad = builder.gogo()
		// .debug()
		// .create()) {
		//
		// // Thread.sleep(100000);
		// }
	}

	@Test
	public void bndrun() throws Exception {
		// try (Launchpad launchpad = builder.bndrun("showit.bndrun")
		// .create()) {
		// // Thread.sleep(100000);
		// }
	}

	@Test
	public void bundles() throws Exception {
		try (Launchpad launchpad = builder.create()) {
			Bundle b = launchpad.bundle()
				.header("FooBar", "1")
				.install();
			String string = b.getHeaders()
				.get("FooBar");
			assertThat(string).isEqualTo("1");
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

			@SuppressWarnings("resource")
			Closeable hide = fw.hide(String.class);
			fw.start();

			boolean isHidden = fw.getServices(String.class)
				.isEmpty();
			assertThat(isHidden).isTrue();

			fw.getFramework()
				.getBundleContext()
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

	@Test
	public void testHidingViaBuilder() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
			.hide(String.class)
			.create()) {

			boolean isHidden = fw.getServices(String.class)
				.isEmpty();

			assertThat(isHidden).isTrue();

			fw.getFramework()
				.getBundleContext()
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

		}

	}

}
