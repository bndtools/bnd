package aQute.xlaunchpad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@SuppressWarnings("restriction")
@RunWith(LaunchpadRunner.class)
public class LaunchpadConfigurationTest {
	static File				tmp		= new File("generated/snapshot");

	static LaunchpadBuilder builder = new LaunchpadBuilder().snapshot()
		.set("snapshot.dir", tmp.getAbsolutePath())
		.bndrun("runsystempackages.bndrun")
		.runfw("jar/org.apache.felix.framework-6.0.2.jar;version=file")
		.export("*")
		.bundles(
			"org.osgi.util.promise, org.osgi.util.function, jar/org.apache.felix.scr-2.1.16.jar;version=file, jar/org.apache.felix.configadmin-1.9.8.jar;version=file, jar/org.apache.felix.configurator-1.0.8.jar;version=file, assertj-core, net.bytebuddy.byte-buddy")
		.debug();

	@Component(name = "TestService", service = X.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
	public static class X {}

	@Service
	Launchpad lp;

	@Test
	public void testConfiguration() throws Exception {

		assertThat(lp.waitForService(X.class, 2000)
			.isPresent()).isFalse();

		Bundle bundle = lp.bundle()
			.addConfiguration(new File("testconfig/test_config.json"))
			.start();

		assertNotNull(bundle);
		assertTrue(bundle.getState() == Bundle.ACTIVE);

		assertThat(lp.waitForService(X.class, 10_000)
			.isPresent()).isTrue();

		bundle.uninstall();

	}

}
