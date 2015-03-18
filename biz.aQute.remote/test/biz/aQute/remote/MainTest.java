package biz.aQute.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.lib.io.IO;
import aQute.remote.main.Envoy;
import aQute.remote.main.Main;
import aQute.remote.plugin.LauncherSupervisor;

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
		IO.delete(IO.getFile("generated/cache"));
		IO.delete(IO.getFile("generated/storage"));
		super.tearDown();
	}

	public void testRemoteMain() throws Exception {
		
		LauncherSupervisor supervisor = new LauncherSupervisor();
		
		supervisor.connect("localhost",
				Envoy.DEFAULT_PORT);

		HashMap<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
				Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		
		List<String> emptyList = Collections.emptyList();

		assertEquals( true, supervisor.getAgent().isEnvoy());
		
		int n = supervisor.getAgent().createFramework("test", emptyList,
				configuration);
		System.out.println(n);
		assertTrue(n > 1024);

		LauncherSupervisor sv = new LauncherSupervisor();
		sv.connect("localhost", n);

		FrameworkDTO framework = sv.getAgent().getFramework();
		assertNotNull(framework);

		sv.getAgent().abort();
		sv.close();

		sv = new LauncherSupervisor();
		sv.connect("localhost", n);

		framework = sv.getAgent().getFramework();
		assertNotNull(framework);

		sv.getAgent().abort();
		sv.close();
	}
	
}
