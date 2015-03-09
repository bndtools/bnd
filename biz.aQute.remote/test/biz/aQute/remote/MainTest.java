package biz.aQute.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.remote.api.Agent;
import aQute.remote.main.Main;
import aQute.remote.plugin.AgentSupervisor;

/**
 * Start the main program which will wait for requests to create a framework.
 */
public class MainTest extends TestCase {

	private Thread thread;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		thread = new Thread() {
			@Override
			public void run() {
				try {
					Main.main(new String[] { "-s", "generated/storage", "-c",
							"generated/cache" });
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	protected void tearDown() throws Exception {
		Main.stop();
		super.tearDown();
	}

	public void testRemoteMain() throws Exception {

		AgentSupervisor supervisor = AgentSupervisor.create("localhost",
				Agent.DEFAULT_PORT);

		HashMap<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
				Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		
		List<String> emptyList = Collections.emptyList();

		assertEquals( Agent.AgentType.envoy, supervisor.getAgent().getType());
		
		int n = supervisor.getAgent().createFramework("test", emptyList,
				configuration);
		System.out.println(n);
		assertTrue(n > 1024);

		AgentSupervisor sv = AgentSupervisor.create("localhost", n);

		FrameworkDTO framework = sv.getAgent().getFramework();
		assertNotNull(framework);

		sv.getAgent().abort();
		sv.close();

		sv = AgentSupervisor.create("localhost", n);

		framework = sv.getAgent().getFramework();
		assertNotNull(framework);

		sv.getAgent().abort();
		sv.close();
	}

}
