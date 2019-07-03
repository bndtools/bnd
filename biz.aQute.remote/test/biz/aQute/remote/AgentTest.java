package biz.aQute.remote;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.dto.RequirementDTO;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;
import junit.framework.TestCase;

public class AgentTest extends TestCase {
	private int				random;
	private File			tmp;
	private Framework		framework;
	private BundleContext	context;
	private Bundle			agent;
	private File			t1;
	private File			t2;
	private File			t3;
	private TestSupervisor	supervisor;

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp");
		IO.delete(tmp);
		tmp.mkdirs();

		t1 = create("bsn-1", new Version(1, 0, 0));
		t2 = create("bsn-2", new Version(2, 0, 0));
		t3 = create("bsn-3", new Version(3, 0, 0));

		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class, this.getClass()
			.getClassLoader());

		FrameworkFactory ff = sl.iterator()
			.next();
		Map<String, String> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		framework = ff.newFramework(configuration);
		framework.init();
		framework.start();
		context = framework.getBundleContext();

		String location = "reference:" + t1.toURI()
			.toString();
		context.installBundle(location);

		location = "reference:" + t2.toURI()
			.toString();
		context.installBundle(location);

		location = "reference:" + IO.getFile("generated/biz.aQute.remote.agent.jar")
			.toURI()
			.toString();
		agent = context.installBundle(location);
		agent.start();
		System.out.println(Arrays.toString(context.getBundles()));
		supervisor = new TestSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT);

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		framework.stop();
		IO.delete(tmp);
		IO.delete(IO.getFile("generated/cache"));
		IO.delete(IO.getFile("generated/storage"));
		framework.waitForStop(100000);
		super.tearDown();
	}

	public void testAgentGetBundles() throws Exception {
		List<BundleDTO> bundles = supervisor.getAgent()
			.getBundles();

		assertNotNull(bundles);
		assertEquals(4, bundles.size());
		assertEquals("bsn-1", bundles.get(1).symbolicName);
		assertEquals("1.0.0", bundles.get(1).version);
		assertEquals("bsn-2", bundles.get(2).symbolicName);
		assertEquals("2.0.0", bundles.get(2).version);

		bundles = supervisor.getAgent()
			.getBundles(1, 2);
		assertEquals(2, bundles.size());
		assertEquals("bsn-1", bundles.get(0).symbolicName);
		assertEquals("1.0.0", bundles.get(0).version);
	}

	public void testAgentGetBundleRevisions() throws Exception {
		List<BundleRevisionDTO> bundleRevisions = supervisor.getAgent()
			.getBundleRevisons();

		assertNotNull(bundleRevisions);
		assertEquals(4, bundleRevisions.size());

		List<RequirementDTO> reqs = bundleRevisions.get(3).requirements; // agent
		assertNotNull(reqs);
		assertTrue(reqs.size() > 0);
		assertEquals("osgi.wiring.package", reqs.get(0).namespace);
	}

	public void testAgentInstallBundle() throws Exception {
		String sha = supervisor.addFile(t3);
		BundleDTO bundle = supervisor.getAgent()
			.install(t3.getAbsolutePath(), sha);

		assertNotNull(bundle);
		assertEquals(4, bundle.id);
		assertEquals("bsn-3", bundle.symbolicName);
		assertEquals("3.0.0", bundle.version);
	}

	public void testAgentInstallBundleWithData() throws Exception {
		byte[] t3data = IO.read(t3);
		BundleDTO bundle = supervisor.getAgent()
			.installWithData(t3.getAbsolutePath(), t3data);

		assertNotNull(bundle);
		assertEquals(4, bundle.id);
		assertEquals("bsn-3", bundle.symbolicName);
		assertEquals("3.0.0", bundle.version);
	}

	public void testAgentInstallBundleFromURL() throws Exception {
		BundleDTO bundle = supervisor.getAgent()
			.installFromURL(t3.getAbsolutePath(), t3.toURI()
				.toURL()
				.toExternalForm());

		assertNotNull(bundle);
		assertEquals(4, bundle.id);
		assertEquals("bsn-3", bundle.symbolicName);
		assertEquals("3.0.0", bundle.version);
		assertEquals(5, supervisor.getAgent()
			.getBundles()
			.size());
	}

	public void testAgentUninstallBundle() throws Exception {
		List<BundleDTO> existingBundles = supervisor.getAgent()
			.getBundles();

		assertNotNull(existingBundles);
		assertTrue(existingBundles.size() == 4);

		assertNull(supervisor.getAgent()
			.uninstall(1));
		assertNull(supervisor.getAgent()
			.uninstall(2));

		existingBundles = supervisor.getAgent()
			.getBundles();

		assertNotNull(existingBundles);
		assertTrue(existingBundles.size() == 2);
	}

	public void testAgentUpdateBundle() throws Exception {
		BundleDTO t2Bundle = supervisor.getAgent()
			.getBundles(2)
			.get(0);

		assertEquals("bsn-2", t2Bundle.symbolicName);
		assertEquals("2.0.0", t2Bundle.version);

		long previousModified = t2Bundle.lastModified;

		File t2prime = create("bsn-2", new Version(2, 0, 1));
		String sha = supervisor.addFile(t2prime);

		assertNull(supervisor.getAgent()
			.update(2, sha));

		t2Bundle = supervisor.getAgent()
			.getBundles(2)
			.get(0);

		assertTrue(previousModified != t2Bundle.lastModified);
	}

	public void testAgentUpdateBundleFromURL() throws Exception {
		BundleDTO t2Bundle = supervisor.getAgent()
			.getBundles(2)
			.get(0);

		assertEquals("bsn-2", t2Bundle.symbolicName);
		assertEquals("2.0.0", t2Bundle.version);

		long previousModified = t2Bundle.lastModified;

		File t2prime = create("bsn-2", new Version(2, 0, 1));

		assertNull(supervisor.getAgent()
			.updateFromURL(t2Bundle.id, t2prime.toURI()
				.toURL()
				.toExternalForm()));

		t2Bundle = supervisor.getAgent()
			.getBundles(2)
			.get(0);

		assertTrue(previousModified != t2Bundle.lastModified);
	}

	// public void testAgentShell() throws Exception {
	// This test requires a gogo to be added to the framework
	// String result = supervisor.getAgent().shell("lb");
	// assertNotNull(result);
	// assertTrue(result.contains("START"));
	// assertTrue(result.contains("LEVEL"));
	// }

	public void testAgentSupervisorTimeout() throws Exception {
		TestSupervisor testSupervisor = new TestSupervisor();

		try {
			testSupervisor.connect("localhost", 12345, 300);
			fail("Excepted connection timeout");
		} catch (ConnectException e) {}

		try {
			testSupervisor.connect("localhost", 12345, 100);
			fail("Excepted connection timeout");
		} catch (ConnectException e) {}

		try {
			testSupervisor.connect("localhost", 12345, -2);
			fail("Excepted illegal argument exception");
		} catch (IllegalArgumentException e) {}
	}

	/**
	 * Launches against main
	 */
	private File create(String bsn, Version v) throws Exception {
		String name = bsn + "-" + v;
		try (Builder b = new Builder()) {
			b.setBundleSymbolicName(bsn);
			b.setBundleVersion(v);
			b.setProperty("Random", random++ + "");
			b.setProperty("-resourceonly", true + "");
			b.setIncludeResource("foo;literal='foo'");
			Jar jar = b.build();
			assertTrue(b.check());

			File file = IO.getFile(tmp, name + ".jar");
			file.getParentFile()
				.mkdirs();
			jar.updateModified(System.currentTimeMillis(), "Force it to now");
			jar.write(file);
			jar.close();

			assertThat(file).isFile()
				.canRead();
			return file;
		}
	}

	static class TestSupervisor extends AgentSupervisor<Supervisor, Agent> implements Supervisor {
		@Override
		public boolean stdout(String out) throws Exception {
			return true;
		}

		@Override
		public boolean stderr(String out) throws Exception {
			return true;
		}

		public void connect(String host, int port) throws Exception {
			super.connect(Agent.class, this, host, port);
		}

		public void connect(String host, int port, int timeout) throws Exception {
			super.connect(Agent.class, this, host, port, timeout);
		}

		@Override
		public void event(Event e) throws Exception {
			System.out.println(e);
		}
	}

}
