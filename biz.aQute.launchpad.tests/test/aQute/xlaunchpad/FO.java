package aQute.xlaunchpad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.Closeable;
import java.io.File;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public class FO {
	static File				tmp		= new File("generated/snapshot");

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().snapshot()
		.set("snapshot.dir", tmp.getAbsolutePath())
		.runfw("org.apache.felix.framework;version='[7.0.5,7.0.5]'")
		.bundles(
			"org.osgi.util.promise, org.osgi.util.function, jar/org.apache.felix.scr-2.1.16.jar;version=file, assertj-core, net.bytebuddy.byte-buddy, org.apache.servicemix.bundles.junit")
		.debug();

	@Component(service = X.class, enabled = false)
	public static class X {}

	@Service
	Launchpad				lp;

	@Service
	ServiceComponentRuntime	runtime;

	@Test
	public void testProperties() throws Exception {
		assertNotNull("a");
		try (Closeable c = lp.enable(X.class)) {
			lp.waitForService(X.class, 1000)
				.get();
		}
		Optional<X> waitForService = lp.waitForService(X.class, 1000);
		assertThat(waitForService.isPresent()).isFalse();
	}
}
