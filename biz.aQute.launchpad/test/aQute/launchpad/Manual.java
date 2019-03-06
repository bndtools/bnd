package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

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

			Bundle start = launchpad.bundle()
				.bundleActivator(Activator.class)
				.start();
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

			ServiceRegistration<Foo> register = launchpad.register(Foo.class, () -> {});
			Optional<Foo> s = launchpad.waitForService(Foo.class, 100);
			assertThat(s.isPresent()).isTrue();

			register.unregister();

			s = launchpad.waitForService(Foo.class, 100);
			assertThat(s.isPresent()).isFalse();

			Optional<Foo> service = launchpad.getService(Foo.class);
		}
	}

	@Test
	public void inject() throws Exception {

		try (Launchpad launchpad = builder.create()) {

			ServiceRegistration<Foo> register = launchpad.register(Foo.class, () -> {});

			class I {
				@Service
				Foo				foo;
				@Service
				Bundle			bundles[];
				@Service
				BundleContext	context;
			}
			I inject = new I();
			launchpad.inject(inject);
			assertThat(inject.bundles).isNotEmpty();
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
			.bundles("org.apache.felix.scr;version='[2.0.10,2.0.10]'")
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
			.bundles("org.apache.felix.scr;version='[2.0.10,2.0.10]'")
			.bundles("org.apache.felix.configadmin")
			.debug()
			.create()) {

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
}
