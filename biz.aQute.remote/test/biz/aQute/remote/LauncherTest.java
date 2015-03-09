package biz.aQute.remote;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.remote.plugin.RemoteProjectLauncherPlugin;

public class LauncherTest extends TestCase {
	private int random;
	private File tmp;
	private Workspace workspace;
	private Project project;
	private HashMap<String, Object> configuration;
	private Framework framework;
	private String location;
	private BundleContext context;
	private Bundle agent;
	private File t1;
	private File t2;

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp");
		tmp.mkdirs();
		IO.copy(IO.getFile("testdata/ws"), tmp);
		workspace = Workspace.getWorkspace(tmp);

		InfoRepository repo = workspace.getPlugin(InfoRepository.class);
		t1 = create("bsn-1", new Version(1, 0, 0));
		t2 = create("bsn-2", new Version(1, 0, 0));

		repo.put(new FileInputStream(t1), null);
		repo.put(new FileInputStream(t2), null);
		t1 = repo.get("bsn-1", new Version(1, 0, 0), null);
		t2 = repo.get("bsn-2", new Version(1, 0, 0), null);
		repo.put(
				new FileInputStream(
						IO.getFile("generated/biz.aQute.remote.launcher-3.0.0.jar")),
				null);

		workspace.getPlugins().add(repo);
		project = workspace.getProject("p1");

		configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
				Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.osgi.framework.launch;version=1.2");
		
		framework = new org.apache.felix.framework.FrameworkFactory()
				.newFramework(configuration);
		framework.init();
		framework.start();
		context = framework.getBundleContext();
		location = "reference:"
				+ IO.getFile("generated/biz.aQute.remote.agent-0.0.0.jar")
						.toURI().toString();
		agent = context.installBundle(location);
		agent.start();

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		agent.stop();
		framework.stop();
		IO.delete(tmp);
	}

	public void testSimpleLauncher() throws Exception {
//		Run bndrun = new Run(workspace, project.getBase(),
//				project.getFile("one.bndrun"));
//		bndrun.setProperty("-runpath", "biz.aQute.remote.launcher");
//		bndrun.setProperty("-runbundles", "bsn-1,bsn-2");
//		bndrun.setProperty("-runremote", "test");
//
//		final RemoteProjectLauncherPlugin pl = (RemoteProjectLauncherPlugin) bndrun
//				.getProjectLauncher();
//
//		final CountDownLatch latch = new CountDownLatch(1);
//		final AtomicInteger exitCode = new AtomicInteger(-1);
//
//		Thread t = new Thread("test-launch") {
//			public void run() {
//				try {
//					exitCode.set(pl.launch());
//				} catch (Exception e) {
//					e.printStackTrace();
//				} finally {
//					latch.countDown();
//				}
//			}
//		};
//		t.start();
//		Thread.sleep(500);
//		for ( Bundle b : context.getBundles()) {
//			System.out.println(b.getLocation());
//		}
//		String p1 = t1.getAbsolutePath();
//		System.out.println(p1);
//		
//		assertNotNull(context.getBundle(p1));
//		assertNotNull(context.getBundle(t2.getAbsolutePath()));
//		
//		pl.cancel();
//		latch.await();
//		
//		assertEquals(-3, exitCode.get());
//		
//		bndrun.close();
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
		file.getParentFile().mkdirs();
		jar.updateModified(System.currentTimeMillis(), "Force it to now");
		jar.write(file);
		b.close();
		return file;
	}

}
