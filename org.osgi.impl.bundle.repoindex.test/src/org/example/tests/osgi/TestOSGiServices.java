package org.example.tests.osgi;

import static org.example.tests.utils.Utils.copyToTempFile;
import static org.example.tests.utils.Utils.createTempDir;
import static org.example.tests.utils.Utils.deleteWithException;
import static org.example.tests.utils.Utils.readStream;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.tests.utils.WibbleAnalyzer;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.log.LogService;

import junit.framework.TestCase;

public class TestOSGiServices extends TestCase {

	private final BundleContext	context	= FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private File				tempDir;

	@Override
	protected void setUp() throws Exception {
		tempDir = createTempDir();
	}

	@Override
	protected void tearDown() throws Exception {
		deleteWithException(tempDir);
	}

	public void testBasicServiceInvocation() throws Exception {
		ServiceReference<ResourceIndexer> ref = context.getServiceReference(ResourceIndexer.class);
		ResourceIndexer indexer = context.getService(ref);

		StringWriter writer = new StringWriter();

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(Collections.singleton(copyToTempFile(tempDir, "testdata/01-bsn+version.jar")), writer,
				config);

		assertEquals(readStream(TestOSGiServices.class.getResourceAsStream("/testdata/fragment-basic.txt")),
				writer.toString().trim());

		context.ungetService(ref);
	}

	// Test whiteboard registration of Resource Analyzers.
	public void testWhiteboardAnalyzer() throws Exception {
		ServiceRegistration<ResourceAnalyzer> reg = context.registerService(ResourceAnalyzer.class,
				new WibbleAnalyzer(), null);

		ServiceReference<ResourceIndexer> ref = context.getServiceReference(ResourceIndexer.class);
		ResourceIndexer indexer = context.getService(ref);
		StringWriter writer = new StringWriter();

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(Collections.singleton(copyToTempFile(tempDir, "testdata/01-bsn+version.jar")), writer,
				config);

		assertEquals(readStream(TestOSGiServices.class.getResourceAsStream("/testdata/fragment-wibble.txt")),
				writer.toString().trim());

		context.ungetService(ref);
		reg.unregister();
	}

	// Test whiteboard registration of Resource Analyzers, with resource filter
	// property.
	public void testWhiteboardAnalyzerWithFilter() throws Exception {
		Dictionary<String,Object> analyzerProps = new Hashtable<>();
		analyzerProps.put(ResourceAnalyzer.FILTER, "(location=*sion.jar)");
		ServiceRegistration<ResourceAnalyzer> reg = context.registerService(ResourceAnalyzer.class,
				new WibbleAnalyzer(), analyzerProps);

		ServiceReference<ResourceIndexer> ref = context.getServiceReference(ResourceIndexer.class);
		ResourceIndexer indexer = context.getService(ref);
		StringWriter writer = new StringWriter();

		Set<File> files = new LinkedHashSet<>();
		files.add(copyToTempFile(tempDir, "testdata/01-bsn+version.jar"));
		files.add(copyToTempFile(tempDir, "testdata/02-localization.jar"));

		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(files, writer, config);

		assertEquals(readStream(TestOSGiServices.class.getResourceAsStream("/testdata/fragment-wibble-filtered.txt")),
				writer.toString().trim());

		context.ungetService(ref);
		reg.unregister();
	}

	// Test that exceptions thrown by analyzers are forwarded to the OSGi Log
	// Service
	public void testLogNotification() throws Exception {
		// Register mock LogService, to receive notifications
		LogService mockLog = mock(LogService.class);
		ServiceRegistration<LogService> mockLogReg = context.registerService(LogService.class, mockLog, null);

		// Register a broken analyzer that throws exceptions
		ResourceAnalyzer brokenAnalyzer = new ResourceAnalyzer() {
			public void analyzeResource(Resource resource, List<Capability> capabilities,
					List<Requirement> requirements) throws Exception {
				throw new Exception("Bang!");
			}
		};
		ServiceRegistration<ResourceAnalyzer> mockAnalyzerReg = context.registerService(ResourceAnalyzer.class,
				brokenAnalyzer, null);

		// Call the indexer
		ServiceReference<ResourceIndexer> ref = context.getServiceReference(ResourceIndexer.class);
		ResourceIndexer indexer = context.getService(ref);
		StringWriter writer = new StringWriter();
		Set<File> files = Collections.singleton(copyToTempFile(tempDir, "testdata/01-bsn+version.jar"));
		Map<String,String> config = new HashMap<>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURI().toString());
		indexer.indexFragment(files, writer, config);

		// Verify log output
		ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
		verify(mockLog).log(any(ServiceReference.class), eq(LogService.LOG_ERROR), anyString(),
				exceptionCaptor.capture());
		assertEquals("Bang!", exceptionCaptor.getValue().getMessage());

		mockAnalyzerReg.unregister();
		mockLogReg.unregister();
	}

}
