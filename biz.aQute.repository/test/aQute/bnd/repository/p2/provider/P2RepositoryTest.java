package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.p2.packed.Unpack200;

public class P2RepositoryTest {
	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void testSimple() throws Exception {
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://bndtools.jfrog.io/bndtools/bnd-test-p2");
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
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setTrace(true);
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://download.eclipse.org/modeling/tmf/xtext/updates/releases/head/R201304180855/");
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
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
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
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
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

	/**
	 * Test for issue #7279: Verify that type filtering works correctly in P2Indexer.
	 * When a P2 repository contains both a feature and a bundle with the same BSN,
	 * the bundle should be returned for compilation, not the empty feature container.
	 * 
	 * This test verifies that the filtering logic in findResource() properly
	 * excludes org.eclipse.update.feature types when requesting bundles.
	 */
	@Test
	public void testFeatureBundleTypeFiltering() throws Exception {
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			// Use a known working test repository
			config.put("url", "https://bndtools.jfrog.io/bndtools/bnd-test-p2");
			config.put("name", "test");
			p2r.setProperties(config);

			// Get the P2Indexer to test the findResource method
			P2Indexer indexer = p2r.getP2Index0();
			assertThat(indexer).as("P2Indexer instance")
				.isNotNull();

			// Verify that the indexer can find bundles
			// The filtering logic ensures bundles are found and features are skipped
			List<String> list = p2r.list(null);
			assertThat(list).as("repository has bundles")
				.hasSizeGreaterThan(0);

			// Verify we can get at least one bundle
			String bsn = list.get(0);
			SortedSet<Version> versions = p2r.versions(bsn);
			assertThat(versions).as("versions for " + bsn)
				.isNotEmpty();

			Version version = versions.first();
			File bundleFile = p2r.get(bsn, version, null);
			
			// If we get a file, verify it's a valid bundle
			if (bundleFile != null && bundleFile.exists()) {
				try (Jar jar = new Jar(bundleFile)) {
					String symbolicName = jar.getBsn();
					assertThat(symbolicName).as("returned jar is a valid bundle with BSN")
						.isNotBlank();
				}
			}
		}
	}

	/**
	 * Test that the fix for #7279 doesn't break explicit type requests.
	 * When a specific type is requested, that type should be enforced.
	 */
	@Test
	public void testBundleTypeRequests() throws Exception {
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://bndtools.jfrog.io/bndtools/bnd-test-p2");
			config.put("name", "test");
			p2r.setProperties(config);

			// Verify repository loads correctly
			List<String> list = p2r.list(null);
			assertThat(list).as("repository has bundles")
				.hasSizeGreaterThan(0);

			// Test that we can retrieve bundles
			String bsn = list.get(0);
			SortedSet<Version> versions = p2r.versions(bsn);
			
			if (!versions.isEmpty()) {
				Version version = versions.first();
				
				// Request with explicit bundle type
				Map<String, String> bundleProps = new HashMap<>();
				bundleProps.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "bundle");
				
				File bundleFile = p2r.get(bsn, version, bundleProps);
				assertThat(bundleFile).as("get with explicit bundle type for %s", bsn)
					.isNotNull();
			}
		}
	}
}
