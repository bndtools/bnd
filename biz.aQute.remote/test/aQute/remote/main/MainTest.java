package aQute.remote.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Run;
import aQute.bnd.exceptions.RunnableWithException;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.remote.agent.AgentDispatcher.Descriptor;
import aQute.remote.api.Agent;
import aQute.remote.main.EnvoyDispatcher.DispatcherInfo;
import aQute.remote.plugin.LauncherSupervisor;

/**
 * Start the main program which will wait for requests to create a framework.
 */
public class MainTest {

	private Thread thread;

	@BeforeEach
	protected void setUp() throws Exception {
		thread = new Thread() {
			@Override
			public void run() {
				try {
					Main.main(new String[] {
						"-p", Agent.DEFAULT_PORT + 1 + "", "-s", "generated/storage", "-c", "generated/cache", "-n",
						"*", "-et", "-Dfoo=bar", "-Dbar=foo"
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.setDaemon(true);
		thread.start();

		long deadline = System.currentTimeMillis() + 5000;
		while (deadline > System.currentTimeMillis()) {
			if (Main.main != null && Main.getDispatcher() != null)
				return;

			Thread.sleep(10);
		}
		throw new IllegalStateException("Cannot initialize agent");
	}

	@AfterEach
	protected void tearDown() throws Exception {
		Main.stop();
		IO.delete(IO.getFile("generated/cache"));
		IO.delete(IO.getFile("generated/storage"));
	}

	@Test
	public void testRemoteMain() throws Exception {

		//
		// Create a framework & start an agent
		//

		LauncherSupervisor supervisor = new LauncherSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT + 1);

		assertEquals(true, supervisor.getAgent()
			.isEnvoy(), "not talking to an envoy");

		HashMap<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, "generated/storage");
		List<String> emptyList = Collections.emptyList();
		boolean created = supervisor.getAgent()
			.createFramework("test", emptyList, configuration);
		assertTrue(created, "there already was a framework, funny, since we created the main in setUp?");

		FrameworkDTO framework = supervisor.getAgent()
			.getFramework();
		assertNotNull(framework, "just created it, so we should have a framework");

		//
		// Create a second supervisor and ensure we do not
		// kill the primary
		//

		LauncherSupervisor sv2 = new LauncherSupervisor();
		sv2.connect("localhost", Agent.DEFAULT_PORT + 1);

		assertTrue(supervisor.getAgent()
			.ping(), "no second framework");

		assertEquals(true, sv2.getAgent()
			.isEnvoy(), "must be an envoy");
		assertFalse(sv2.getAgent()
			.createFramework("test", emptyList, configuration), "the framework should already exist");

		assertTrue(supervisor.getAgent()
			.ping(), "first framework is gone");

		FrameworkDTO fw2 = sv2.getAgent()
			.getFramework();
		assertEquals(framework.properties.get("org.osgi.framework.uuid"), fw2.properties.get("org.osgi.framework.uuid"),
			"we should not have created a new framework");

		//
		// Kill the second framework
		//

		supervisor.getAgent()
			.abort();
		Thread.sleep(500);
		assertFalse(supervisor.isOpen());

		assertTrue(sv2.getAgent()
			.ping(), "should not have killed sv2");

		sv2.abort();
		Thread.sleep(500);
		assertFalse(sv2.isOpen());
	}

	@Test
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
