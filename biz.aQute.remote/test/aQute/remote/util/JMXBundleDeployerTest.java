package aQute.remote.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests have JDK dependencies so are ignored
 */
public class JMXBundleDeployerTest {

	@Test
	@Ignore
	public void testFindManagementAgentJar() throws Exception {
		File managementJar = JMXBundleDeployer.findJdkJar("management-agent.jar");
		assertNotNull(managementJar);
		assertEquals(true, managementJar.exists());
	}

	@Test
	@Ignore
	public void testFindToolsJar() throws Exception {
		File toolsJar = JMXBundleDeployer.findJdkJar("tools.jar");
		assertNotNull(toolsJar);
		assertEquals(true, toolsJar.exists());
	}

	@Test
	@Ignore
	public void testGetLocalConnectorAddressLibraryUnload() throws Exception {
		Throwable ex = null;

		// calling two times to make sure that we are properly unloading the
		// libattach.so library. If we don't the second call will throw class
		// init error
		try {
			JMXBundleDeployer.getLocalConnectorAddress();
			JMXBundleDeployer.getLocalConnectorAddress();
		} catch (Throwable t) {
			ex = t;
		}

		assertNull(ex);
	}

}
