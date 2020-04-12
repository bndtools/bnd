package aQute.bnd.repository.p2.provider;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.resource.Resource;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.p2.packed.Unpack200;

public class P2RepositoryTest {
	File tmp;

	@BeforeEach
	public void setUp(TestInfo info) throws Exception {
		Method testMethod = info.getTestMethod()
			.get();
		tmp = Paths.get("generated/tmp/test", getClass().getName(), testMethod.getName())
			.toAbsolutePath()
			.toFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);
	}

	@Test
	public void testSimple() throws Exception {
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://dl.bintray.com/bndtools/bndtools/latest/");
			config.put("name", "test");
			p2r.setProperties(config);

			List<String> list = p2r.list(null);
			assertThat(list).as("list(null)")
				.hasSizeGreaterThan(1);
		}
	}

	@Test
	public void testXtext() throws Exception {

		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setTrace(true);
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "http://download.eclipse.org/modeling/tmf/xtext/updates/releases/head/R201304180855/");
			config.put("name", "test");
			p2r.setProperties(config);
			List<String> list = p2r.list(null);
			assertThat(w.check()).as("Workspace check")
				.isTrue();
			assertThat(list).as("list(null)")
				.hasSizeGreaterThan(1);
		}
	}

	@EnabledIfUnpack200 // https://openjdk.java.net/jeps/367
	@Test
	public void testPack200Async() throws Exception {
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setTrace(true);
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.21.0/");
			config.put("name", "test");
			p2r.setProperties(config);
			List<String> list = p2r.list(null);
			assertThat(w.check()).as("Workspace check")
				.isTrue();
			assertThat(list).as("list(null)")
				.hasSizeGreaterThan(1);

			SortedSet<Version> versions = p2r.versions("org.eclipse.xtext");
			assertThat(versions).as("versions(\"org.eclipse.xtext\")")
				.isNotEmpty();
			final AtomicReference<File> asyncResult = new AtomicReference<>();
			final CountDownLatch countDownLatch = new CountDownLatch(1);
			p2r.get("org.eclipse.xtext", versions.last(), null, new DownloadListener() {

				@Override
				public void success(File file) throws Exception {
					asyncResult.set(file);
					countDownLatch.countDown();
				}

				@Override
				public void failure(File file, String reason) throws Exception {
					System.out.println(reason);
					countDownLatch.countDown();
				}

				@Override
				public boolean progress(File file, int percentage) throws Exception {
					return true;
				}
			});
			assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).as("wait for async download")
				.isTrue();
			File file = asyncResult.get();
			assertThat(file).as("get(\"org.eclipse.xtext\", %s)", versions.last())
				.isNotNull();

			// Make sure file is valid jar
			try (Jar jar = new Jar(file)) {
				assertThat(Version.parseVersion(jar.getVersion())).as("jar version")
					.isEqualTo(versions.last());
			}
			Resource resource = p2r.getP2Index0()
				.getBridge()
				.get("org.eclipse.xtext", versions.last());
			assertThat(resource).as("get(\"org.eclipse.xtext\", %s)", versions.last())
				.isNotNull();
			ContentCapability contentCapability = ResourceUtils.getContentCapability(resource);
			assertThat(contentCapability).as("content capability")
				.isNotNull();
			URI url = contentCapability.url();
			assertThat(url.getPath()).as("content capability url path")
				.endsWith(Unpack200.PACKED_SUFFIX);

			HttpClient client = w.getPlugin(HttpClient.class);
			File cacheFile = client.getCacheFileFor(url);
			File originalCacheFile = new File(cacheFile.getParentFile(), cacheFile.getName() + ".original");
			assertThat(originalCacheFile).as("originalCacheFile")
				.isFile();
		}
	}

	@EnabledIfUnpack200 // https://openjdk.java.net/jeps/367
	@Test
	public void testPack200() throws Exception {
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setTrace(true);
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.21.0/");
			config.put("name", "test");
			p2r.setProperties(config);
			List<String> list = p2r.list(null);
			assertThat(w.check()).as("Workspace check")
				.isTrue();
			assertThat(list).as("list(null)")
				.hasSizeGreaterThan(1);

			SortedSet<Version> versions = p2r.versions("org.eclipse.xtext");
			assertThat(versions).as("versions(\"org.eclipse.xtext\")")
				.isNotEmpty();
			File file = p2r.get("org.eclipse.xtext", versions.last(), null);
			assertThat(file).as("get(\"org.eclipse.xtext\", %s)", versions.last())
				.isNotNull();

			// Make sure file is valid jar
			try (Jar jar = new Jar(file)) {
				assertThat(Version.parseVersion(jar.getVersion())).as("jar version")
					.isEqualTo(versions.last());
			}
			Resource resource = p2r.getP2Index0()
				.getBridge()
				.get("org.eclipse.xtext", versions.last());
			assertThat(resource).as("get(\"org.eclipse.xtext\", %s)", versions.last())
				.isNotNull();
			ContentCapability contentCapability = ResourceUtils.getContentCapability(resource);
			assertThat(contentCapability).as("content capability")
				.isNotNull();
			URI url = contentCapability.url();
			assertThat(url.getPath()).as("content capability url path")
				.endsWith(Unpack200.PACKED_SUFFIX);

			HttpClient client = w.getPlugin(HttpClient.class);
			File cacheFile = client.getCacheFileFor(url);
			File originalCacheFile = new File(cacheFile.getParentFile(), cacheFile.getName() + ".original");
			assertThat(originalCacheFile).as("originalCacheFile")
				.isFile();
		}
	}
}
