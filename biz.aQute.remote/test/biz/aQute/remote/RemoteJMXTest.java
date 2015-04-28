package biz.aQute.remote;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.framework.dto.*;
import org.osgi.framework.launch.*;

import aQute.lib.io.*;
import aQute.remote.util.*;

public class RemoteJMXTest extends TestCase {

	private Map<String,Object>	configuration;
	private Framework			framework;
	private File				tmp;

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp");
		tmp.mkdirs();

		configuration = new HashMap<String,Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.osgi.framework.launch;version=1.4");
		framework = new org.apache.felix.framework.FrameworkFactory().newFramework(configuration);
		framework.init();
		framework.start();
		BundleContext context = framework.getBundleContext();

		String[] bundles = {
				"../cnf/repo/osgi.cmpn/osgi.cmpn-4.3.1.jar",
				"testdata/slf4j-simple-1.7.12.jar", "testdata/slf4j-api-1.7.12.jar",
				"testdata/org.apache.aries.util-1.1.0.jar", "testdata/org.apache.aries.jmx-1.1.1.jar",
				"generated/biz.aQute.remote.test.jmx-3.0.0.jar"
		};

		for (String bundle : bundles) {
			String location = "reference:" + IO.getFile(bundle).toURI().toString();
			Bundle b = context.installBundle(location);
			if (!bundle.contains("slf4j-simple")) {
				b.start();
			}
		}

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		framework.stop();
		framework.waitForStop(10000);
		IO.delete(tmp);
		super.tearDown();
	}

	public void testJMXBundleDeployer() throws Exception {
		JMXBundleDeployer jmxBundleDeployer = new JMXBundleDeployer();

		long bundleId = jmxBundleDeployer.deploy("biz.aQute.remote.agent",
				IO.getFile("generated/biz.aQute.remote.agent-3.0.0.jar"));

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
