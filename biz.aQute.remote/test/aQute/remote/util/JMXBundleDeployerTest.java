package aQute.remote.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

public class JMXBundleDeployerTest {

	@Test
	public void testFindManagementAgentJar() throws Exception {
		File managementJar = JMXBundleDeployer.findJdkJar("management-agent.jar");
		assertNotNull(managementJar);
		assertEquals(true, managementJar.exists());
	}

	@Test
	public void testFindToolsJar() throws Exception {
		File toolsJar = JMXBundleDeployer.findJdkJar("tools.jar");
		assertNotNull(toolsJar);
		assertEquals(true, toolsJar.exists());
	}

}
