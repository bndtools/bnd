package biz.aQute.remote;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.RunSession;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.remote.main.Main;
import aQute.remote.plugin.RemoteProjectLauncherPlugin;
import aQute.remote.plugin.RunSessionImpl;
import junit.framework.TestCase;

/**
 * Creates a workspace and then launches a main remote.
 */
public class LauncherTest extends TestCase {
	private int						random;
	private File					tmp;
	private Workspace				workspace;
	private HashMap<String, Object>	configuration;
	private Framework				framework;
	private String					location;
	private BundleContext			context;
	private Bundle					agent;
	private File					t1;
	private File					t2;
	private Thread					thread;

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + getTestName());
		IO.delete(tmp);
		IO.mkdirs(tmp);
		IO.copy(IO.getFile("testdata/ws"), tmp);
		workspace = Workspace.getWorkspace(tmp);
		workspace.refresh();

		InfoRepository repo = workspace.getPlugin(InfoRepository.class);
		t1 = create("bsn-1", new Version(1, 0, 0));
		t2 = create("bsn-2", new Version(1, 0, 0));

		repo.put(new FileInputStream(t1), null);
		repo.put(new FileInputStream(t2), null);
		t1 = repo.get("bsn-1", new Version(1, 0, 0), null);
		t2 = repo.get("bsn-2", new Version(1, 0, 0), null);
		repo.put(new FileInputStream(IO.getFile("generated/biz.aQute.remote.launcher.jar")), null);

		workspace.getPlugins()
			.add(repo);

		File storage = IO.getFile("generated/storage-1");
		storage.mkdirs();

		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());

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

		thread = new Thread() {
			@Override
			public void run() {
				try {
					Main.main(new String[] {
						"-s", "generated/storage", "-c", "generated/cache", "-p", "1090", "-et"
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.setDaemon(true);
		thread.start();

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		framework.stop();
		Main.stop();
		IO.delete(IO.getFile("generated/cache"));
		IO.delete(IO.getFile("generated/storage"));
		framework.waitForStop(100000);
		super.tearDown();
	}

	/*
	 * Launches against the agent
	 */
	public void testSimpleLauncher() throws Exception {
		Project project = workspace.getProject("p1");
		Run bndrun = new Run(workspace, project.getBase(), project.getFile("one.bndrun"));
		bndrun.setProperty("-runpath", "biz.aQute.remote.launcher");
		bndrun.setProperty("-runbundles", "bsn-1,bsn-2");
		bndrun.setProperty("-runremote", "test");

		final RemoteProjectLauncherPlugin pl = (RemoteProjectLauncherPlugin) bndrun.getProjectLauncher();
		pl.prepare();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger exitCode = new AtomicInteger(-1);

		List<? extends RunSession> sessions = pl.getRunSessions();
		assertEquals(1, sessions.size());

		final RunSession session = sessions.get(0);

		Thread t = new Thread("test-launch") {
			@Override
			public void run() {
				try {
					exitCode.set(session.launch());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			}
		};
		t.start();
		Thread.sleep(500);

		for (Bundle b : context.getBundles()) {
			System.out.println(b.getLocation());
		}
		assertEquals(4, context.getBundles().length);
		String p1 = t1.getAbsolutePath();
		System.out.println(p1);

		assertNotNull(context.getBundle(p1));
		assertNotNull(context.getBundle(t2.getAbsolutePath()));

		pl.cancel();
		latch.await();

		assertEquals(-3, exitCode.get());

		bndrun.close();
	}

	/**
	 * Launches against main
	 */
	/*
	 * Launches against the agent& main
	 */
	public void testMain() throws Exception {
		Project project = workspace.getProject("p1");
		Run bndrun = new Run(workspace, project.getBase(), project.getFile("one.bndrun"));
		bndrun.setProperty("-runpath", "biz.aQute.remote.launcher");
		bndrun.setProperty("-runbundles", "bsn-1,bsn-2");
		bndrun.setProperty("-runremote", "main;agent=1090");

		final RemoteProjectLauncherPlugin pl = (RemoteProjectLauncherPlugin) bndrun.getProjectLauncher();
		pl.prepare();

		List<? extends RunSession> sessions = pl.getRunSessions();
		assertEquals(1, sessions.size());

		RunSessionImpl main = (RunSessionImpl) sessions.get(0);

		CountDownLatch mainLatch = launch(main);

		main.waitTillStarted(1000);

		assertEquals(0, main.started.getCount());
		Thread.sleep(500);

		main.cancel();

		mainLatch.await();
		assertEquals(-3, main.getExitCode());

		bndrun.close();
	}

	/*
	 * Launches against the agent& main
	 */
	public void testAgentAndMain() throws Exception {
		Project project = workspace.getProject("p1");
		Run bndrun = new Run(workspace, project.getBase(), project.getFile("one.bndrun"));
		bndrun.setProperty("-runpath", "biz.aQute.remote.launcher");
		bndrun.setProperty("-runbundles", "bsn-1,bsn-2");
		bndrun.setProperty("-runremote", "agent,main;agent=1090");

		final RemoteProjectLauncherPlugin pl = (RemoteProjectLauncherPlugin) bndrun.getProjectLauncher();
		pl.prepare();

		List<? extends RunSession> sessions = pl.getRunSessions();
		assertEquals(2, sessions.size());

		RunSession agent = sessions.get(0);
		RunSession main = sessions.get(1);

		CountDownLatch agentLatch = launch(agent);
		CountDownLatch mainLatch = launch(main);

		agent.waitTillStarted(1000);
		main.waitTillStarted(1000);
		Thread.sleep(500);

		agent.cancel();
		main.cancel();

		agentLatch.await();
		mainLatch.await();
		assertEquals(-3, agent.getExitCode());
		assertEquals(-3, main.getExitCode());

		bndrun.close();
	}

	private CountDownLatch launch(final RunSession session) {
		final CountDownLatch latch = new CountDownLatch(1);

		Thread t = new Thread("test-launch") {
			@Override
			public void run() {
				try {
					session.launch();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			}
		};
		t.start();
		return latch;
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

}
