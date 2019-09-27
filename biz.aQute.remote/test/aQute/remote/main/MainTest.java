package aQute.remote.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Run;
import aQute.bnd.osgi.Processor;
import aQute.lib.exceptions.RunnableWithException;
import aQute.lib.io.IO;
import aQute.remote.agent.AgentDispatcher.Descriptor;
import aQute.remote.api.Agent;
import aQute.remote.main.EnvoyDispatcher.DispatcherInfo;
import aQute.remote.plugin.LauncherSupervisor;
import junit.framework.TestCase;

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
					Main.main(new String[] {
						"-p", Agent.DEFAULT_PORT + 1 + "", "-s", "generated/storage", "-c", "generated/cache", "-n",
						"*", "-et"
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.setDaemon(true);
		thread.start();

		while (Main.main == null) {
			Thread.sleep(100);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		Main.stop();
		IO.delete(IO.getFile("generated/cache"));
		IO.delete(IO.getFile("generated/storage"));
		super.tearDown();
	}

	public void testRemoteMain() throws Exception {

		//
		// Create a framework & start an agent
		//

		LauncherSupervisor supervisor = new LauncherSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT + 1);

		assertEquals("not talking to an envoy", true, supervisor.getAgent()
			.isEnvoy());

		HashMap<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, "generated/storage");
		List<String> emptyList = Collections.emptyList();
		boolean created = supervisor.getAgent()
			.createFramework("test", emptyList, configuration);
		assertTrue("there already was a framework, funny, since we created the main in setUp?", created);

		FrameworkDTO framework = supervisor.getAgent()
			.getFramework();
		assertNotNull("just created it, so we should have a framework", framework);

		//
		// Create a second supervisor and ensure we do not
		// kill the primary
		//

		LauncherSupervisor sv2 = new LauncherSupervisor();
		sv2.connect("localhost", Agent.DEFAULT_PORT + 1);

		assertTrue("no second framework", supervisor.getAgent()
			.ping());

		assertEquals("must be an envoy", true, sv2.getAgent()
			.isEnvoy());
		assertFalse("the framework should already exist", sv2.getAgent()
			.createFramework("test", emptyList, configuration));

		assertTrue("first framework is gone", supervisor.getAgent()
			.ping());

		FrameworkDTO fw2 = sv2.getAgent()
			.getFramework();
		assertEquals("we should not have created a new framework", framework.properties.get("org.osgi.framework.uuid"),
			fw2.properties.get("org.osgi.framework.uuid"));

		//
		// Kill the second framework
		//

		supervisor.getAgent()
			.abort();
		Thread.sleep(500);
		assertFalse(supervisor.isOpen());

		assertTrue("should not have killed sv2", sv2.getAgent()
			.ping());

		sv2.abort();
		Thread.sleep(500);
		assertFalse(sv2.isOpen());
	}

	public void testStartlevels() throws Exception {
		EnvoyDispatcher dispatcher = Main.getDispatcher();

		try (Run bndrun = Run.createRun(null, IO.getFile("startlevels.bndrun"))) {

			assertTrue(bndrun.check());

			try (ProjectLauncher l = bndrun.getProjectLauncher()) {
				// assertTrue(bndrun.getWorkspace()
				// .check());
				assertTrue(bndrun.check());
				assertTrue(l.check());

				Processor.getExecutor()
					.execute(RunnableWithException.asRunnable(l::launch));

				await().until(() -> dispatcher.getDispatcherInfo("test") != null);

				DispatcherInfo info = dispatcher.getDispatcherInfo("test");
				Descriptor descriptor = (Descriptor) info.framework;
				assertThat(descriptor).isNotNull();

				assertThat(descriptor.closed).isFalse();

				assertThat(descriptor.startlevels.hasStartLevels()).isTrue();

				descriptor.startlevels.sync();

				assertThat(descriptor.startlevels.getFrameworkStartLevel(descriptor.framework)).isEqualTo(32);

				BundleContext context = descriptor.framework.getBundleContext();
				assertThat(descriptor.startlevels.getBundleStartLevel(context.getBundle(1))).isEqualTo(10);
				assertThat(descriptor.startlevels.getBundleStartLevel(context.getBundle(2))).isEqualTo(30);
				assertThat(descriptor.startlevels.getBundleStartLevel(context.getBundle(3))).isEqualTo(31);

				bndrun.setProperty(aQute.bnd.osgi.Constants.RUNBUNDLES + ".x", "org.apache.felix.gogo.command");

				l.update();

			}

		}
	}

}
