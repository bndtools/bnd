package biz.aQute.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.launch.Framework;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.remote.util.JMXBundleDeployer;

public class RemoteJMXTest {

	private Map<String, Object>	configuration;
	private Framework			framework;
	@InjectTemporaryDirectory
	File						tmp;

	@BeforeEach
	protected void setUp() throws Exception {
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.osgi.framework.launch;version=1.4");
		framework = new org.apache.felix.framework.FrameworkFactory().newFramework(configuration);
		framework.init();
		framework.start();
		BundleContext context = framework.getBundleContext();

		String[] bundles = {
			"testdata/osgi.cmpn-4.3.1.jar", "testdata/slf4j-simple-1.7.12.jar", "testdata/slf4j-api-1.7.12.jar",
			"testdata/org.apache.aries.util-1.1.0.jar", "testdata/org.apache.aries.jmx-1.1.1.jar",
			"generated/biz.aQute.remote.test.jmx.jar"
		};

		for (String bundle : bundles) {
			String location = "reference:" + IO.getFile(bundle)
				.toURI()
				.toString();
			Bundle b = context.installBundle(location);
			if (!bundle.contains("slf4j-simple")) {
				b.start();
			}
		}
	}

	@AfterEach
	protected void tearDown() throws Exception {
		framework.stop();
		framework.waitForStop(10000);
	}

	@Test
	public void testJMXBundleDeployer() throws Exception {
		JMXBundleDeployer jmxBundleDeployer = new JMXBundleDeployer();

		long bundleId = jmxBundleDeployer.deploy("biz.aQute.remote.agent",
			IO.getFile("generated/biz.aQute.remote.agent.jar"));

		assertTrue(bundleId > 0);

		BundleDTO agentBundle = null;
		long state = -1;

		for (BundleDTO bundle : jmxBundleDeployer.listBundles()) {
			if (bundle.symbolicName.equals("biz.aQute.remote.agent")) {
				agentBundle = bundle;
				state = bundle.state;
			}
		}

		assertNotNull(agentBundle);
		assertEquals(Bundle.ACTIVE, state);
	}

}
