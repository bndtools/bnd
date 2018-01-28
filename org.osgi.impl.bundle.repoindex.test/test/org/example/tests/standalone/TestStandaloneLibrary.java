package org.example.tests.standalone;

import static org.example.tests.utils.Utils.copyToTempFile;
import static org.example.tests.utils.Utils.createTempDir;
import static org.example.tests.utils.Utils.deleteWithException;
import static org.example.tests.utils.Utils.readStream;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.service.log.LogService;

import aQute.lib.io.IO;
import junit.framework.TestCase;

public class TestStandaloneLibrary extends TestCase {

	public void testBasicServiceInvocationWithRelativePaths() throws Exception {
		IO.delete(IO.getFile("generated/temp"));

		assertIndexPath("generated/temp", "generated/temp/foo.jar");
		assertIndexPath("generated/temp", "bin/../generated/temp/../../generated/temp/foo.jar");
		assertIndexPath("src/../generated/temp/../temp", "bin/../generated/temp/../../generated/temp/foo.jar");
		assertIndexPath("src/../generated/temp/../temp", "generated/temp/foo.jar");

		assertIndexPath("generated/temp", "generated/temp/abc/foo.jar", "abc/foo.jar");
		assertIndexPath("generated/temp", "bin/../generated/temp/../../generated/temp/abc/foo.jar", "abc/foo.jar");
		assertIndexPath("src/../generated/temp/../temp", "bin/../generated/temp/../../generated/temp/abc/foo.jar",
				"abc/foo.jar");
		assertIndexPath("src/../generated/temp/../temp", "generated/temp/abc/foo.jar", "abc/foo.jar");

		assertIndexPath("generated/temp", "generated/temp/bar/foo.jar", "../temp/bar/foo.jar");
		assertIndexPath("generated/temp", "bin/../generated/temp/../../generated/temp/bar/foo.jar",
				"../temp/bar/foo.jar");
		assertIndexPath("src/../generated/temp/../temp", "bin/../generated/temp/../../generated/temp/bar/foo.jar",
				"../temp/bar/foo.jar");
		assertIndexPath("src/../generated/temp/../temp", "generated/temp/bar/foo.jar", "../temp/bar/foo.jar");
	}

	private void assertIndexPath(String root, String file) throws Exception {
		assertIndexPath(root, file, "foo.jar");
	}

	private void assertIndexPath(String root, String file, String dest) throws Exception {
		File tempDir = IO.getFile(root);
		tempDir.mkdirs();
		final List<String> errors = new ArrayList<>();
		final List<String> warnings = new ArrayList<>();

		File target = IO.getFile(tempDir, dest);
		target.getParentFile().mkdirs();

		try {
			IO.copy(IO.getFile("src/testdata/01-bsn+version.jar"), target);
			ResourceIndexer indexer = new RepoIndex(new LogService() {

				@Override
				public void log(ServiceReference sr, int level, String message, Throwable exception) {
					log(level, message + " " + exception);
				}

				@Override
				public void log(ServiceReference sr, int level, String message) {
					log(level, message);
				}

				@Override
				public void log(int level, String message, Throwable exception) {
					log(level, message + " " + exception);
				}

				@Override
				public void log(int level, String message) {
					switch (level) {
						case LogService.LOG_ERROR :
							errors.add(message);
							break;
						case LogService.LOG_WARNING :
							warnings.add(message);
							break;
						default :
							break;
					}
				}
			});

			StringWriter writer = new StringWriter();

			String osRootPath = root.replace('/', File.separatorChar);
			String rootURI = new File(osRootPath).toURI().toString();
			File osFile = new File(file.replace('/', File.separatorChar));
			assertTrue(osFile + " does not exist", osFile.isFile());

			Map<String,String> config = new HashMap<>();
			config.put(ResourceIndexer.ROOT_URL, rootURI);

			indexer.indexFragment(Collections.singleton(osFile), writer, config);

			assertEquals(0, errors.size());
			assertEquals(0, warnings.size());

		} finally {
			deleteWithException(tempDir);
		}
	}

	public void testBasicServiceInvocation() throws Exception {
		ResourceIndexer indexer = new RepoIndex();

		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/01-bsn+version.jar");

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);

		assertEquals(readStream(TestStandaloneLibrary.class.getResourceAsStream("/testdata/fragment-basic.txt")),
				writer.toString().trim());

		deleteWithException(tempDir);
	}

	public void testKnownBundleRecognition() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));

		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);

		assertEquals(
				readStream(TestStandaloneLibrary.class
						.getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0.fragment.txt")),
				writer.toString().trim());

		deleteWithException(tempDir);
	}

	public void testKnownBundlesExtra() throws Exception {
		Properties extras = new Properties();
		extras.setProperty("org.eclipse.equinox.ds;[1.4,1.5)", "cap=extra;extra=wibble");

		KnownBundleAnalyzer knownBundlesAnalyzer = new KnownBundleAnalyzer();
		knownBundlesAnalyzer.setKnownBundlesExtra(extras);

		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(knownBundlesAnalyzer, FrameworkUtil.createFilter("(name=*)"));

		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);

		assertEquals(
				readStream(TestStandaloneLibrary.class
						.getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0.extra-fragment.txt")),
				writer.toString().trim());

		deleteWithException(tempDir);
	}

	public void testPlainJar() throws Exception {
		RepoIndex indexer = new RepoIndex();

		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/jcip-annotations.jar");

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);

		assertEquals(readStream(TestStandaloneLibrary.class.getResourceAsStream("/testdata/plainjar.fragment.txt")),
				writer.toString().trim());

		deleteWithException(tempDir);
	}

}
