package aQute.remote.util;

import java.io.File;

import junit.framework.TestCase;

public class JMXBundleDeployerTest extends TestCase {

	public void testFindManagementAgentJar() throws Exception {
		File managementJar = JMXBundleDeployer.findJdkJar("management-agent.jar");
		assertNotNull(managementJar);
		assertEquals(true, managementJar.exists());
	}

	public void testFindToolsJar() throws Exception {
		File toolsJar = JMXBundleDeployer.findJdkJar("tools.jar");
		assertNotNull(toolsJar);
		assertEquals(true, toolsJar.exists());
	}

	// HAngs
	// public void testGetLocalConnectorAddressLibraryUnload() throws Exception
	// {
	// Throwable ex = null;
	//
	// // calling two times to make sure that we are properly unloading the
	// // libattach.so library. If we don't the second call will throw class
	// // init error
	// try {
	// JMXBundleDeployer.getLocalConnectorAddress();
	// JMXBundleDeployer.getLocalConnectorAddress();
	// } catch (Throwable t) {
	// ex = t;
	// }
	//
	// assertNull(ex);
	// }

}
