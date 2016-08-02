package aQute.bnd.repository.osgi;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.version.Version;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.provider.FakeNexus;
import junit.framework.TestCase;

public class OSGiRepositoryTest extends TestCase {
	File				tmp		= IO.getFile("generated/tmp");
	File				cache	= IO.getFile(tmp, "cache");
	File				remote	= IO.getFile("testdata");
	File				ws		= IO.getFile(tmp, "ws");
	private FakeNexus	fnx;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IO.delete(tmp);
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
	}

	public void testSimple() throws Exception {
		OSGiRepository r = new OSGiRepository();
		Map<String,String> map = new HashMap<>();
		map.put("locations", fnx.getBaseURI("/repo/minir5.xml").toString());
		map.put("cache", "generated/tmp/cache");
		map.put("max.stale", "10000");
		r.setProperties(map);
		Processor p = new Processor();
		final AtomicInteger tasks = new AtomicInteger();

		p.addBasicPlugin(new ProgressPlugin() {

			@Override
			public Task startTask(final String name, int size) {
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
			}
		});
		HttpClient httpClient = new HttpClient();
		httpClient.setRegistry(p);
		p.addBasicPlugin(httpClient);
		p.setBase(ws);
		p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
		r.setRegistry(p);
		r.setReporter(new Slf4jReporter());

		assertEquals(0, tasks.get());
		File file = r.get("dummybundle", new Version("0"), null);
		assertNotNull(file);
		assertEquals(2, tasks.get()); // 2 = index + file
		File file2 = r.get("dummybundle", new Version("0"), null);
		assertNotNull(file2);
		// second one should not have downloaded
		assertEquals(2, tasks.get());

		r.refresh();
		File file3 = r.get("dummybundle", new Version("0"), null);
		assertNotNull(file3);
		// second one should not have downloaded
		assertEquals(2, tasks.get());
	}

	public void testBndRepo() throws Exception {
		OSGiRepository r = new OSGiRepository();
		Map<String,String> map = new HashMap<>();
		map.put("locations",
				"https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/artifact/dist/bundles/index.xml.gz");
		map.put("cache", "generated/tmp/cache");
		map.put("max.stale", "10000");
		r.setProperties(map);
		Processor p = new Processor();
		final AtomicInteger tasks = new AtomicInteger();

		p.addBasicPlugin(new ProgressPlugin() {

			@Override
			public Task startTask(final String name, int size) {
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
			}
		});
		HttpClient httpClient = new HttpClient();
		httpClient.setRegistry(p);
		p.addBasicPlugin(httpClient);
		p.setBase(ws);
		p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
		r.setRegistry(p);
		r.setReporter(new Slf4jReporter());

		assertEquals(0, tasks.get());
		List<String> list = r.list(null);
		assertFalse(list.isEmpty());

		SortedSet<Version> versions = r.versions("aQute.libg");
		assertFalse(versions.isEmpty());
		File f1 = r.get("aQute.libg", versions.first(), null);
		assertNotNull(f1);
		File f2 = r.get("aQute.libg", versions.first(), null);
	}

}
