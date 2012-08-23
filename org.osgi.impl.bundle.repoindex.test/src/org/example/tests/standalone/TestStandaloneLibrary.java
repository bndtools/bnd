package org.example.tests.standalone;

import static org.example.tests.utils.Utils.*;

import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.framework.FrameworkUtil;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;

public class TestStandaloneLibrary extends TestCase {

	public void testBasicServiceInvocation() throws Exception {
		ResourceIndexer indexer = new RepoIndex();
		
		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/01-bsn+version.jar");

		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);
		
		assertEquals(readStream(TestStandaloneLibrary.class.getResourceAsStream("/testdata/fragment-basic.txt")), writer.toString().trim());
		
		deleteWithException(tempDir);
	}
	
	public void testKnownBundleRecognition() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));
		
		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");

		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);
		
		assertEquals(readStream(TestStandaloneLibrary.class.getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0.fragment.txt")), writer.toString().trim());
		
		deleteWithException(tempDir);
	}
	
	public void testKnownBundlesExtra() throws Exception {
		Properties extras = new Properties();
		extras.setProperty("org.eclipse.equinox.ds;[1.4,1.5)", "cap@extra;extra=wibble");
		
		KnownBundleAnalyzer knownBundlesAnalyzer = new KnownBundleAnalyzer();
		knownBundlesAnalyzer.setKnownBundlesExtra(extras);
		
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(knownBundlesAnalyzer, FrameworkUtil.createFilter("(name=*)"));
		
		StringWriter writer = new StringWriter();
		File tempDir = createTempDir();
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");

		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(Collections.singleton(tempFile), writer, config);
		
		assertEquals(readStream(TestStandaloneLibrary.class.getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0.extra-fragment.txt")), writer.toString().trim());
		
		deleteWithException(tempDir);	}

}
