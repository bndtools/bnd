package aQute.xlaunchpad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.File;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import aQute.libg.parameters.ParameterMap;

@SuppressWarnings("restriction")
@RunWith(LaunchpadRunner.class)
public class LaunchpadRunnerBasicTest {
	static File				tmp		= new File("generated/snapshot");

	static LaunchpadBuilder builder = new LaunchpadBuilder().snapshot()
		.set("snapshot.dir", tmp.getAbsolutePath())
		.bndrun("runsystempackages.bndrun")
		.runfw("jar/org.apache.felix.framework-6.0.2.jar;version=file")
		.export("*")
		.bundles(
			"org.osgi.util.promise, org.osgi.util.function, jar/org.apache.felix.scr-2.1.16.jar;version=file, assertj-core, net.bytebuddy.byte-buddy")
		.debug();

	@Component(service = X.class, enabled = false)
	public static class X {}

	@Service
	Launchpad				lp;

	@Service
	ServiceComponentRuntime	scr;

	@Test
	public void testProperties() throws Exception {

		assertThat(lp.waitForService(X.class, 100L)).isEmpty();

		try (Closeable c = lp.enable(X.class)) {
			assertThat(lp.waitForService(X.class, 5000L)).isNotEmpty();
		}
		assertThat(lp.waitForService(X.class, 100L)).isEmpty();
	}

	@Test
	public void testProperties2() throws Exception {

		assertThat(lp.waitForService(X.class, 100L)).isEmpty();

		try (Closeable c = lp.enable(X.class)) {
			assertThat(lp.waitForService(X.class, 5000L)).isNotEmpty();
		}
		assertThat(lp.waitForService(X.class, 100L)).isEmpty();
	}

	@Test
	public void testRunsystemPackages() throws Exception {
		Set<String> p = new ParameterMap(lp.getBundleContext()
			.getProperty("org.osgi.framework.system.packages.extra")).keySet();
		assertThat(p).contains("sun.misc");
	}
}
