package aQute.bnd.repository.osgi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.progress.ProgressPlugin.Task;
import aQute.bnd.version.Version;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.maven.provider.FakeNexus;
import junit.framework.TestCase;

@SuppressWarnings("deprecation")
public class OSGiRepositoryTest extends TestCase {
	File				tmp;
	File				cache;
	File				remote;
	File				ws;
	private FakeNexus	fnx;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmp = IO.getFile("generated/tmp/test/" + getClass().getName() + "/" + getName());
		cache = IO.getFile(tmp, "cache");
		remote = IO.getFile(tmp, "testdata");
		ws = IO.getFile(tmp, "ws");
		IO.delete(tmp);
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
		remote.mkdirs();
		IO.copy(IO.getFile("testdata/minir5.xml"), IO.getFile(remote, "minir5.xml"));
		IO.copy(IO.getFile("testdata/bundles"), IO.getFile(remote, "bundles"));
	}

	@Override
	protected void tearDown() throws Exception {
		IO.close(fnx);
	}

	public void testSimple() throws Exception {
		try (OSGiRepository r = new OSGiRepository()) {
			assertTrue(testRepo(r));
		}
	}

	@SuppressWarnings("deprecation")
	public void testCompatibilityWithFixedIndexedRepo() throws Exception {
		try (aQute.bnd.deployer.repository.FixedIndexedRepo r = new aQute.bnd.deployer.repository.FixedIndexedRepo()) {
			assertTrue(testRepo(r,
				"FixedIndexedRepository is deprecated, please use aQute.bnd.repository.osgi.OSGiRepository"));
		}
	}

	private boolean testRepo(OSGiRepository r, String... checks) throws URISyntaxException, Exception {
		Map<String, String> map = new HashMap<>();
		map.put("locations", fnx.getBaseURI("/repo/minir5.xml")
			.toString());
		map.put("cache", cache.getPath());
		map.put("max.stale", "10000");
		r.setProperties(map);
		try (Processor p = new Processor(); HttpClient httpClient = new HttpClient()) {
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			try (Workspace workspace = Workspace.createStandaloneWorkspace(p, ws.toURI())) {
				p.addBasicPlugin(workspace);
				r.setRegistry(p);

				final AtomicInteger tasks = new AtomicInteger();

				p.addBasicPlugin((ProgressPlugin) (name, size) -> {
					System.out.println("Starting " + name);
					tasks.incrementAndGet();
					return new Task() {

						@Override
						public void worked(int units) {
							System.out.println("Worked " + name + " " + units);
						}

						@Override
						public void done(String message, Throwable e) {
							System.out.println("Done " + name + " " + message);
						}

						@Override
						public boolean isCanceled() {
							return false;
						}
					};
				});

				assertThat(tasks).hasValue(0);
				File file = r.get("dummybundle", new Version("0"), null);
				assertThat(file).exists();
				assertThat(tasks).hasValue(2); // 2 = index + file
				File file2 = r.get("dummybundle", new Version("0"), null);
				assertThat(file2).exists();
				// second one should not have downloaded
				assertThat(tasks).hasValue(2);
				r.getIndex(false);
				File file3 = r.get("dummybundle", new Version("0"), null);
				assertThat(file3).exists();
				// second one should not have downloaded
				assertThat(tasks).hasValue(2);

				p.getInfo(workspace);
				return p.check(checks);
			}
		}
	}

	public void testNoPolling() throws Exception {
		try (Processor p = new Processor(); Workspace workspace = Workspace.createStandaloneWorkspace(p, ws.toURI())) {
			workspace.setProperty(Constants.GESTALT, Constants.GESTALT_BATCH);
			testPolling(workspace, false);
		}
	}

	public void testPolling() throws Exception {
		try (Processor p = new Processor(); Workspace workspace = Workspace.createStandaloneWorkspace(p, ws.toURI())) {
			testPolling(workspace, true);
		}
	}

	private void testPolling(Workspace workspace, boolean polled) throws Exception {
		try (OSGiRepository r = new OSGiRepository();
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient()) {
			Map<String, String> map = new HashMap<>();
			map.put("locations", fnx.getBaseURI("/repo/minir5.xml")
				.toString());
			map.put("cache", cache.getPath());
			map.put("max.stale", "10000");
			map.put("name", "test");
			map.put("poll.time", "1");
			r.setProperties(map);
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(workspace);
			r.setRegistry(p);
			final AtomicReference<RepositoryPlugin> refreshed = new AtomicReference<>();
			CountDownLatch latch = new CountDownLatch(1);
			p.addBasicPlugin(new RepositoryListenerPlugin() {

				@Override
				public void repositoryRefreshed(RepositoryPlugin repository) {
					refreshed.set(repository);
					latch.countDown();
				}

				@Override
				public void repositoriesRefreshed() {}

				@Override
				public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {}

				@Override
				public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {}
			});
			File file = r.get("dummybundle", new Version("0"), null);
			assertThat(file).exists();
			assertThat(r.title()).isNull(); // not stale, default name

			System.out.println("1");
			assertThat(latch.await(3, TimeUnit.SECONDS)).as("waiting for RepositoryListenerPlugin notification")
				.isFalse();
			System.out.println("2");
			assertThat(refreshed).hasValue(null);
			System.out.println("3");
			// update the index file
			File index = IO.getFile(remote, "minir5.xml");
			long time = index.lastModified();

			String s = IO.collect(index);
			s += " "; // change the sha
			IO.store(s, index);
			System.out.println("5 " + index + " " + (index.lastModified() - time));
			assertThat(latch.await(10, TimeUnit.SECONDS)).as("waiting for RepositoryListenerPlugin notification")
				.isEqualTo(polled); // give the poller a chance
			System.out.println("6");

			if (polled) {
				assertThat(refreshed).hasValue(r);
				assertThat(r.title()).isEqualTo("test [stale]");
			}
			System.out.println(r.tooltip());
		}
	}

	public void testPollingWithFile() throws Exception {
		try (OSGiRepository r = new OSGiRepository();
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient()) {
			Map<String, String> map = new HashMap<>();
			map.put("locations", IO.getFile(remote, "minir5.xml")
				.toURI()
				.toString());
			map.put("cache", cache.getPath());
			map.put("max.stale", "10000");
			map.put("name", "test");
			map.put("poll.time", "1");
			r.setProperties(map);
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
			r.setRegistry(p);
			final AtomicReference<RepositoryPlugin> refreshed = new AtomicReference<>();
			CountDownLatch latch = new CountDownLatch(1);
			p.addBasicPlugin(new RepositoryListenerPlugin() {

				@Override
				public void repositoryRefreshed(RepositoryPlugin repository) {
					refreshed.set(repository);
					latch.countDown();
				}

				@Override
				public void repositoriesRefreshed() {}

				@Override
				public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {}

				@Override
				public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {}
			});

			File file = r.get("dummybundle", new Version("0"), null);
			assertThat(file).exists();

			assertThat(latch.await(3, TimeUnit.SECONDS)).as("waiting for RepositoryListenerPlugin notification")
				.isFalse();
			assertThat(refreshed).hasValue(null);

			System.out.println("1");
			// update the index file
			File index = IO.getFile(remote, "minir5.xml");
			long time = index.lastModified();
			do {
				Thread.sleep(1000);
				String s = IO.collect(index);
				s += " "; // change the sha
				IO.store(s, index);
				System.out.printf("%tc%n", index.lastModified());
			} while (index.lastModified() == time);

			System.out.println("2 ");
			assertThat(latch.await(10, TimeUnit.SECONDS)).as("waiting for RepositoryListenerPlugin notification")
				.isTrue();
			System.out.println("3 ");

			assertThat(refreshed).hasValue(r);
			assertThat(r.title()).isEqualTo("test [stale]");
			System.out.println(r.tooltip());
		}
	}

	public void testRefreshable() throws Exception {
		try (OSGiRepository testRepo = new OSGiRepository();
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient()) {
			Map<String, String> map = new HashMap<>();
			File index = IO.getFile(remote, "minir5.xml");
			map.put("locations", index.toURI()
				.toString());
			map.put("cache", cache.getPath());
			map.put("max.stale", "10000");
			map.put("name", "test");
			map.put("poll.time", "-1");
			testRepo.setProperties(map);
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
			testRepo.setRegistry(p);
			long indexLastModifiedBeforeRefresh = testRepo.getIndex(false)
				.getCache()
				.lastModified();
			AtomicInteger numberOfTimesRefreshed = new AtomicInteger();
			final AtomicReference<RepositoryPlugin> refreshedRepo = new AtomicReference<>();
			p.addBasicPlugin(new RepositoryListenerPlugin() {

				@Override
				public void repositoryRefreshed(RepositoryPlugin repository) {
					numberOfTimesRefreshed.incrementAndGet();
					refreshedRepo.set(repository);
				}

				@Override
				public void repositoriesRefreshed() {}

				@Override
				public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {}

				@Override
				public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {}
			});
			// update the index file
			long time = index.lastModified();
			do {
				Thread.sleep(1000);
				String s = IO.collect(index);
				s += " "; // change the sha
				IO.store(s, index);
			} while (index.lastModified() == time);
			// refresh through Refreshable interface
			boolean refreshed = testRepo.refresh(true);
			// give the Promise time to resolve
			Thread.sleep(1000);
			assertTrue("The cache should have been modified after the refresh", testRepo.getIndex(false)
				.getCache()
				.lastModified() > indexLastModifiedBeforeRefresh);
			assertTrue("The refresh method should return true", refreshed);
			assertEquals("Exactly 1 repository should have been refreshed", 1, numberOfTimesRefreshed.get());
			assertEquals("The repository that has been refreshed should be the created one", testRepo,
				refreshedRepo.get());
		}
	}

	public void testBndRepo() throws Exception {
		try (OSGiRepository r = new OSGiRepository();
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient()) {
			Map<String, String> map = new HashMap<>();
			map.put("locations", "https://dl.bintray.com/bnd/dist/4.1.0/index.xml.gz");
			map.put("cache", cache.getPath());
			map.put("max.stale", "10000");
			r.setProperties(map);
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
			r.setRegistry(p);

			final AtomicInteger tasks = new AtomicInteger();

			p.addBasicPlugin((ProgressPlugin) (name, size) -> {
				System.out.println("Starting " + name);
				tasks.incrementAndGet();
				return new Task() {

					@Override
					public void worked(int units) {
						System.out.println("Worked " + name + " " + units);
					}

					@Override
					public void done(String message, Throwable e) {
						System.out.println("Done " + name + " " + message);
					}

					@Override
					public boolean isCanceled() {
						return false;
					}
				};
			});

			assertThat(tasks).hasValue(0);
			List<String> list = r.list(null);
			assertThat(list).isNotEmpty();

			SortedSet<Version> versions = r.versions("aQute.libg");
			assertThat(versions).isNotEmpty();
			File f1 = r.get("aQute.libg", versions.first(), null);
			assertThat(f1).isNotNull();
			assertThat(tasks).hasValueGreaterThanOrEqualTo(2); // index + bundle
																// + redirects
			int t = tasks.get();

			File f2 = r.get("aQute.libg", versions.first(), null);
			assertThat(tasks).hasValue(t); // should use cache

			r.getIndex(true);
			File f3 = r.get("aQute.libg", versions.first(), null);
			assertThat(tasks).hasValue(t * 2); // should fetch again

		}
	}

}
