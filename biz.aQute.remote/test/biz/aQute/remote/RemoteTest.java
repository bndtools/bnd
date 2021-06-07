package biz.aQute.remote;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.remote.api.Agent;
import aQute.remote.plugin.LauncherSupervisor;
import junit.framework.TestCase;

public class RemoteTest extends TestCase {
	private int						random;
	private HashMap<String, Object>	configuration;
	private Framework				framework;
	private BundleContext			context;
	private Bundle					agent;
	private String					location;
	private File					tmp;

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		tmp = IO.getFile("generated/tmp/test/" + getTestName());
		IO.delete(tmp);
		IO.mkdirs(tmp);
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.osgi.framework.launch;version=1.2");

		framework = new org.apache.felix.framework.FrameworkFactory().newFramework(configuration);
		framework.init();
		framework.start();
		context = framework.getBundleContext();
		location = "reference:" + IO.getFile("generated/biz.aQute.remote.agent.jar")
			.toURI()
			.toString();
		agent = context.installBundle(location);
		agent.start();

	}

	@Override
	protected void tearDown() throws Exception {
		framework.stop();
		framework.waitForStop(10000);
		super.tearDown();
	}

	public void testSimple() throws Exception {
		LauncherSupervisor supervisor = new LauncherSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT);
		assertNotNull(supervisor);

		Agent agent = supervisor.getAgent();
		assertNotNull(agent.getFramework());

		// Create stdin/stderr buffers
		// and redirect output

		StringBuffer stderr = new StringBuffer();
		StringBuffer stdout = new StringBuffer();
		supervisor.setStderr(stderr);
		supervisor.setStdout(stdout);
		supervisor.redirect(1);

		//
		// Install the bundle systemio
		//
		File f = IO.getFile("generated/biz.aQute.remote.test.systemio.jar");
		String sha = supervisor.addFile(f);
		BundleDTO bundle = agent.install(f.getAbsolutePath(), sha);

		//
		// Start the bundle and capture the output
		//

		String result = agent.start(bundle.id);
		assertNull(result, result);

		Thread.sleep(1000);
		assertEquals("Hello World", stdout.toString()
			.trim());
		stdout.setLength(0);

		// Send input (will be consumed by the Activator.stop
		ByteArrayInputStream bin = new ByteArrayInputStream(new String("Input\n").getBytes());
		supervisor.setStdin(bin);

		// stop the bundle (will return input as uppercase)
		result = agent.stop(bundle.id);
		assertNull(result, result);
		Thread.sleep(1000);
		assertEquals("INPUT", stdout.toString()
			.trim());

	}

	public void testUpdate() throws Exception {
		LauncherSupervisor supervisor = new LauncherSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT);

		File t1 = create("bsn-1", new Version(1, 0, 0));
		File t2 = create("bsn-2", new Version(1, 0, 0));
		assertTrue(t1.isFile());
		assertTrue(t2.isFile());

		String sha1 = supervisor.addFile(t1);
		String sha2 = supervisor.addFile(t2);

		Map<String, String> update = new HashMap<>();
		update.put(t1.getAbsolutePath(), sha1);

		String errors = supervisor.getAgent()
			.update(update);
		assertNull(errors);

		//
		// Verify that t1 is installed and t2 not
		//

		Bundle b1 = context.getBundle(t1.getAbsolutePath());
		assertNotNull(b1);
		Bundle b2 = context.getBundle(t2.getAbsolutePath());
		assertNull(b2);

		//
		// Now add a new one
		//

		update = new HashMap<>();
		update.put(t1.getAbsolutePath(), sha1);
		update.put(t2.getAbsolutePath(), sha2);
		errors = supervisor.getAgent()
			.update(update);
		assertNull(errors);
		assertNotNull(context.getBundle(t1.getAbsolutePath()));
		assertNotNull(context.getBundle(t2.getAbsolutePath()));

		//
		// Now change a bundle
		//

		t1 = create("bsn-1", new Version(2, 0, 0));
		sha1 = supervisor.addFile(t1);
		update = new HashMap<>();
		update.put(t1.getAbsolutePath(), sha1);
		update.put(t2.getAbsolutePath(), sha2);
		errors = supervisor.getAgent()
			.update(update);
		assertNull(errors);
		b1 = context.getBundle(t1.getAbsolutePath());
		assertNotNull(b1);
		b2 = context.getBundle(t2.getAbsolutePath());
		assertNotNull(b2);
		assertEquals(new Version(2, 0, 0).toString(), b1.getVersion()
			.toString());

		assertEquals(Bundle.ACTIVE, b1.getState());
		assertEquals(Bundle.ACTIVE, b2.getState());

		//
		// Now delete t1
		//

		update = new HashMap<>();
		update.put(t2.getAbsolutePath(), sha2);
		errors = supervisor.getAgent()
			.update(update);
		assertNull(errors);
		assertNull(context.getBundle(t1.getAbsolutePath()));
		assertNotNull(context.getBundle(t2.getAbsolutePath()));

		//
		// Delete all
		//
		supervisor.getAgent()
			.update(null);
		assertNull(context.getBundle(t1.getAbsolutePath()));
		assertNull(context.getBundle(t2.getAbsolutePath()));
	}

	public void testUpdateOrder() throws Exception {
		LauncherSupervisor supervisor = new LauncherSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT);

		List<String> bundles = new ArrayList<>();
		LinkedHashMap<String, String> update = new LinkedHashMap<>();

		for (int i = 0; i < 50; i++) {
			String name = UUID.randomUUID()
				.toString();
			File f = create(name, new Version(1, 0, 0));
			assertTrue(f.isFile());

			String sha = supervisor.addFile(f);

			update.put(f.getAbsolutePath(), sha);
			bundles.add(name);
		}

		String errors = supervisor.getAgent()
			.update(update);
		assertNull(errors);

		//
		// Now check installed bundle order
		//
		Bundle[] installed = context.getBundles();
		for (int i = 2; i < installed.length; i++) {
			Bundle b = installed[i];
			assertTrue(b.getLocation()
				.endsWith(bundles.get(i - 2) + "-1.0.0.jar"));
		}

		// delete
		supervisor.getAgent()
			.update(null);
	}

	private File create(String bsn, Version v) throws Exception {
		String name = bsn + "-" + v;
		Builder b = new Builder();
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
		b.close();
		return file;
	}

	/*
	 * Test if we can get the BundleRevisionDTOs
	 */

	public void testBRD() throws Exception {
		LauncherSupervisor supervisor = new LauncherSupervisor();
		supervisor.connect("localhost", Agent.DEFAULT_PORT);
		assertNotNull(supervisor);

		Agent agent = supervisor.getAgent();
		assertNotNull(agent);

		List<BundleRevisionDTO> bundleRevisons = agent.getBundleRevisons();
		assertNotNull(bundleRevisons);

	}
}
