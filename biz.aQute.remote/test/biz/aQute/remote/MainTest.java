package biz.aQute.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.remote.agent.provider.Main;
import aQute.remote.api.Agent;
import aQute.remote.api.Envoy;
import aQute.remote.supervisor.provider.SupervisorClient;

public class MainTest extends TestCase {

	private Thread thread;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		thread = new Thread() {
			@Override
			public void run() {
				try {
					Main.main(new String[] {"-s","generated/storage", "-c", "generated/cache"});
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
		super.tearDown();
	}

	public void testRemoteMain() throws Exception {

		SupervisorClient<Envoy> supervisor = SupervisorClient.link(Envoy.class,
				"localhost", Envoy.DEFAULT_PORT);

		HashMap<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
				Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		List<String> emptyList = Collections.emptyList();

		int n = supervisor.getAgent().createFramework("test",emptyList, configuration);
		System.out.println(n);
		assertTrue(n > 1024);
		
		SupervisorClient<Agent> sv = SupervisorClient.link(Agent.class, "localhost", n);
		
		FrameworkDTO framework = sv.getAgent().getFramework();
		assertNotNull(framework);
		
		sv.getAgent().abort();
		sv.close();
		
		sv = SupervisorClient.link(Agent.class, "localhost", n);
		
		framework = sv.getAgent().getFramework();
		assertNotNull(framework);
		
		sv.getAgent().abort();
		sv.close();
	}

}
