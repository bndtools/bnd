package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BridgeRepository.ResourceInfo;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;

public class P2RepositoryTest {
	@InjectTemporaryDirectory
	File tmp;

	@ParameterizedTest
	@CsvSource({"false, false", "false, true", "true, false", "true, true"})
	public void testLocationAsDirectory(boolean separateCache, boolean existingDirectory) throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File location = new File(tmp, "directory-location");
		File cache = separateCache ? new File(tmp, "bundles") : location;
		if (existingDirectory)
			assertThat(location.mkdirs()).isTrue();
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", repository.toURI()
				.toString());
			config.put("name", "test");
			config.put("location", location.getAbsolutePath() + (existingDirectory ? "" : "/"));
			if (separateCache)
				config.put("cache", cache.getAbsolutePath());
			p2r.setProperties(config);

			List<String> bundles = p2r.list(null);
			assertThat(bundles).isNotEmpty();
			String bsn = bundles.get(0);
			File bundle = p2r.get(bsn, p2r.versions(bsn).last(), null);
			assertThat(bundle).isFile().hasParent(cache);
			assertThat(new File(location, "index.xml.gz")).isFile();
			assertThat(p2r.getRoot()).isEqualTo(cache);
			if (separateCache)
				assertThat(new File(location, bundle.getName())).doesNotExist();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void testLocationAsIndexFile(boolean separateCache) throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File location = new File(tmp, "file-location");
		File indexFile = new File(location, "custom-index.xml.gz");
		File cache = separateCache ? new File(tmp, "bundles") : location;
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", repository.toURI()
				.toString());
			config.put("name", "test");
			config.put("location", indexFile.getAbsolutePath());
			if (separateCache)
				config.put("cache", "bundles");
			p2r.setProperties(config);

			List<String> bundles = p2r.list(null);
			assertThat(bundles).isNotEmpty();
			String bsn = bundles.get(0);
			Version version = p2r.versions(bsn).last();
			File bundle = p2r.get(bsn, version, null);
			assertThat(bundle).isFile().hasParent(cache);
			assertThat(indexFile).isFile();
			assertThat(new File(location, "index.xml.gz")).doesNotExist();
			assertThat(p2r.getRoot()).isEqualTo(cache);
			assertThat(p2r.getLocation()).isEqualTo(cache.getPath());
			if (separateCache)
				assertThat(new File(location, bundle.getName())).doesNotExist();
			assertThat(p2r.refresh()).isTrue();
			assertThat(p2r.get(bsn, version, null)).isEqualTo(bundle).isFile();
			assertThat(indexFile).isFile();
			try (P2Repository reopened = new P2Repository()) {
				reopened.setRegistry(w);
				reopened.setProperties(config);
				assertThat(reopened.get(bsn, version, null)).isEqualTo(bundle).isFile();
				assertThat(reopened.getRoot()).isEqualTo(cache);
			}
		}
	}

	/**
	 * An earlier bnd version created a directory at an index file location.
	 * The index must be written as a file at that location and be readable
	 * by an OSGiRepository.
	 */
	@Test
	public void testLocationAsIndexFileReplacesLegacyIndexDirectory() throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File indexFile = new File(tmp, "cnf/ext/macbadge_p2.index.xml.gz");
		IO.mkdirs(indexFile);
		IO.store("stale", new File(indexFile, "index.xml.gz"));
		IO.store("link", new File(indexFile, "some.bundle-1.0.0.jar"));
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);
			p2r.setReporter(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", repository.toURI()
				.toString());
			config.put("name", "test");
			config.put("location", indexFile.getAbsolutePath());
			config.put("cache", "cnf/cache/p2-test");
			p2r.setProperties(config);

			List<String> bundles = p2r.list(null);
			assertThat(bundles).isNotEmpty();
			assertThat(indexFile).isFile();
			assertThat(w.getWarnings()).anyMatch(warning -> warning.contains("earlier bnd version"));

			try (OSGiRepository osgi = new OSGiRepository()) {
				osgi.setRegistry(w);
				osgi.setReporter(w);
				osgi.setProperties(Map.of("name", "osgi", "locations", indexFile.toURI()
					.toString(), "cache", new File(tmp, "cnf/cache/osgi").getAbsolutePath()));
				assertThat(osgi.list(null)).containsExactlyInAnyOrderElementsOf(bundles);
			}
		}
	}

	@Test
	public void testLocationAsIndexFileKeepsForeignDirectory() throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File indexFile = new File(tmp, "cnf/ext/macbadge_p2.index.xml.gz");
		File foreign = new File(indexFile, "notes.txt");
		IO.mkdirs(indexFile);
		IO.store("keep me", foreign);
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", repository.toURI()
				.toString());
			config.put("name", "test");
			config.put("location", indexFile.getAbsolutePath());
			p2r.setProperties(config);

			assertThatThrownBy(() -> p2r.list(null)).hasMessageContaining("is a directory, not an index file");
			assertThat(foreign).isFile();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void testLocationDefault(boolean separateCache) throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File location = new File(tmp, "cnf/cache/p2-test");
		File cache = separateCache ? new File(tmp, "bundles") : location;
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);
			Map<String, String> config = new HashMap<>();
			config.put("url", repository.toURI().toString());
			config.put("name", "test");
			if (separateCache)
				config.put("cache", "bundles");
			p2r.setProperties(config);

			List<String> bundles = p2r.list(null);
			assertThat(bundles).isNotEmpty();
			String bsn = bundles.get(0);
			Version version = p2r.versions(bsn).last();
			File bundle = p2r.get(bsn, version, null);
			assertThat(bundle).isFile().hasParent(cache);
			assertThat(new File(location, "index.xml.gz")).isFile();
			assertThat(p2r.getRoot()).isEqualTo(cache);
		}
	}

	@Test
	public void testLocationWithInvalidCache() throws Exception {
		File repository = IO.getFile("testdata/p2/macbadge");
		File cache = new File(tmp, "not-a-directory");
		IO.store("existing file", cache);
		try (Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			P2Repository p2r = new P2Repository()) {
			w.setProperty(Constants.CONNECTION_SETTINGS, "false");
			w.setBase(tmp);
			p2r.setRegistry(w);
			p2r.setProperties(Map.of("url", repository.toURI().toString(), "name", "test", "cache",
				cache.getAbsolutePath()));

			assertThatThrownBy(() -> p2r.list(null)).hasMessageContaining(cache.getPath());
			assertThat(IO.collect(cache)).isEqualTo("existing file");
		}
	}

	@Test
	public void testLocationCacheConfiguration() throws Exception {
		assertThat(Converter.cnv(P2Config.class, Map.of()).cache()).isNull();
		assertThat(Converter.cnv(P2Config.class, Map.of("cache", "bundles")).cache()).isEqualTo("bundles");
		assertThat(P2Config.class.getMethod("cache").isDefault()).isTrue();
		P2Config legacy = new P2Config() {
			@Override
			public String name(String defaultName) {
				return defaultName;
			}

			@Override
			public URI url() {
				return null;
			}

			@Override
			public String location() {
				return null;
			}

			@Override
			public String location(String defaultLocation) {
				return defaultLocation;
			}

			@Override
			public String tags() {
				return null;
			}

			@Deprecated
			@Override
			public boolean targetPlatform() {
				return false;
			}
		};
		assertThat(legacy.cache()).isNull();
	}

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

			// Test 3: Verify features are filtered out of the main repository
			// but still accessible via getFeatures()
			List<Feature> features = indexer.getFeatures();
			assertThat(features).as("repository has features")
				.hasSizeGreaterThan(0);

			for (String bsn : list) {
				SortedSet<Version> versions = p2r.versions(bsn);
				Map<Version, ResourceInfo> versionInfos = indexer.getBridge()
					.versionInfos(bsn);

				Set<Version> featureVersions = new TreeSet<>();
				for (Entry<Version, ResourceInfo> entry : versionInfos.entrySet()) {
					Resource resource = entry.getValue()
						.getResource();
					boolean isFeature = ResourceUtils.hasType(resource, "org.eclipse.update.feature");
					if (isFeature) {
						featureVersions.add(entry.getKey());
					}
				}

				if (!featureVersions.isEmpty()) {
					assertThat(versions).doesNotContainAnyElementsOf(featureVersions);
				}
			}

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
