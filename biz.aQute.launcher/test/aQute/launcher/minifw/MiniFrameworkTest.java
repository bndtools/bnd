package aQute.launcher.minifw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

public class MiniFrameworkTest {

	@Test
	public void testSimple() throws Exception {
		Properties properties = new Properties();
		try (MiniFramework framework = new MiniFramework(properties)) {
			assertThat(framework.getState()).isEqualTo(Bundle.RESOLVED);

			URL file = new File("../demo/generated/demo.jar").getCanonicalFile()
				.toURI()
				.toURL();
			assertThatCode(() -> file.openStream()
				.close()).doesNotThrowAnyException();

			framework.init();
			assertThat(framework.getState()).isEqualTo(Bundle.STARTING);
			assertThat(framework.getBundleId()).isEqualTo(Constants.SYSTEM_BUNDLE_ID);

			FrameworkWiring frameworkWiring = framework.adapt(FrameworkWiring.class);
			assertThat(frameworkWiring).isNotNull();
			assertThat(frameworkWiring.getBundle()
				.getBundleId()).isEqualTo(framework.getBundleId());

			FrameworkStartLevel frameworkStartLevel = framework.adapt(FrameworkStartLevel.class);
			assertThat(frameworkStartLevel).isNotNull();
			assertThat(frameworkStartLevel.getBundle()
				.getBundleId()).isEqualTo(framework.getBundleId());

			Bundle b = framework.installBundle("reference:" + file.toExternalForm());
			assertThat(b).isNotNull();
			assertThat(b.getBundleId()).isGreaterThan(framework.getBundleId());
			assertThat(b.getSymbolicName()).isEqualTo("demo");
			assertThat(b.getHeaders()
				.get(Constants.BUNDLE_SYMBOLICNAME)).isEqualTo("demo");

			BundleContext context = b.getBundleContext();
			assertThat(context).isNotNull();
			assertThat(context.getBundle()
				.getBundleId()).isEqualTo(b.getBundleId());

			assertThat(context.getBundles()).hasSize(2);
			assertThat(framework.getBundles()).hasSize(2);

			assertThat(framework.getBundle()).isLessThan(b);
			assertThat(b).isGreaterThan(framework.getBundle());

			assertThat(context.getProperty(Constants.FRAMEWORK_UUID)).isNotNull()
			.isEqualTo(framework.getProperty(Constants.FRAMEWORK_UUID));

			Class<?> c = b.loadClass("test.TestActivator");
			assertThat(c).isNotNull();
			assertThat(BundleActivator.class).isAssignableFrom(c);

			BundleRevision revision = b.adapt(BundleRevision.class);
			assertThat(revision).isNotNull();
			assertThat(revision.getBundle()
				.getBundleId()).isEqualTo(b.getBundleId());
			assertThat(revision.getTypes()).isZero();

			BundleWiring wiring = b.adapt(BundleWiring.class);
			assertThat(wiring).isNotNull();
			assertThat(wiring.getBundle()
				.getBundleId()).isEqualTo(b.getBundleId());
			BundleRevision wiringRevision = wiring.getRevision();
			assertThat(wiringRevision).isNotNull();
			assertThat(wiringRevision.getBundle()
				.getBundleId()).isEqualTo(b.getBundleId());

			ClassLoader loader = wiring.getClassLoader();
			assertThat(loader).isInstanceOf(BundleReference.class);
			BundleReference reference = (BundleReference) loader;
			assertThat(reference.getBundle()
				.getBundleId()).isEqualTo(b.getBundleId());

			BundleStartLevel startLevel = b.adapt(BundleStartLevel.class);
			assertThat(startLevel).isNotNull();
			assertThat(startLevel.getBundle()
				.getBundleId()).isEqualTo(b.getBundleId());

			URL url = b.getEntry("META-INF/MANIFEST.MF");
			assertThat(url).isNotNull();
			Manifest manifest;
			try (InputStream in = url.openStream()) {
				assertThat(in).isNotNull();
				manifest = new Manifest(in);
			} catch (IOException e) {
				throw Assertions.<AssertionError> fail("unable to read manifest", e);
			}
			assertThat(manifest).isNotNull();
			assertThat(manifest.getMainAttributes()
				.getValue(Constants.BUNDLE_SYMBOLICNAME)).isEqualTo("demo");

			framework.start();
			assertThat(framework.getState()).isEqualTo(Bundle.ACTIVE);

			AtomicInteger called = new AtomicInteger(0);
			frameworkWiring.refreshBundles(Collections.singletonList(b), (FrameworkEvent event) -> {
				assertThat(event).isNotNull();
				assertThat(event.getType()).isEqualTo(FrameworkEvent.PACKAGES_REFRESHED);
				called.incrementAndGet();
			});
			assertThat(called).hasValue(1);

			called.set(0);
			frameworkStartLevel.setStartLevel(2, (FrameworkEvent event) -> {
				assertThat(event).isNotNull();
				assertThat(event.getType()).isEqualTo(FrameworkEvent.STARTLEVEL_CHANGED);
				called.incrementAndGet();
			});
			assertThat(called).hasValue(1);

			FrameworkEvent waitForStop = framework.waitForStop(10);
			assertThat(waitForStop).isNotNull();
			assertThat(waitForStop.getType()).isEqualTo(FrameworkEvent.WAIT_TIMEDOUT);

			framework.stop();
			assertThat(framework.getState()).isEqualTo(Bundle.RESOLVED);

			waitForStop = framework.waitForStop(10);
			assertThat(waitForStop).isNotNull();
			assertThat(waitForStop.getType()).isEqualTo(FrameworkEvent.STOPPED);
		}
	}
}
