package org.example.tests.osgi;

import static org.example.tests.utils.Utils.*;

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

import junit.framework.TestCase;

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

import static org.mockito.Mockito.*;

public class TestOSGiServices extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
	private File tempDir;
	
	@Override
	protected void setUp() throws Exception {
		tempDir = createTempDir();
	}
	
	@Override
	protected void tearDown() throws Exception {
		deleteWithException(tempDir);
	}
	
	public void testBasicServiceInvocation() throws Exception {
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		
		StringWriter writer = new StringWriter();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(Collections.singleton(copyToTempFile(tempDir, "testdata/01-bsn+version.jar")), writer, config);
		
		assertEquals(readStream(TestOSGiServices.class.getResourceAsStream("/testdata/fragment-basic.txt")), writer.toString().trim());
		
		context.ungetService(ref);
	}
	
	// Test whiteboard registration of Resource Analyzers.
	public void testWhiteboardAnalyzer() throws Exception {
		ServiceRegistration reg = context.registerService(ResourceAnalyzer.class.getName(), new WibbleAnalyzer(), null);
		
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		StringWriter writer = new StringWriter();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(Collections.singleton(copyToTempFile(tempDir, "testdata/01-bsn+version.jar")), writer, config);
		
		assertEquals(readStream(TestOSGiServices.class.getResourceAsStream("/testdata/fragment-wibble.txt")), writer.toString().trim());
		
		context.ungetService(ref);
		reg.unregister();
	}

	// Test whiteboard registration of Resource Analyzers, with resource filter property.
	public void testWhiteboardAnalyzerWithFilter() throws Exception {
		Dictionary<String, Object> analyzerProps = new Hashtable<String, Object>();
		analyzerProps.put(ResourceAnalyzer.FILTER, "(location=*sion.jar)");
		ServiceRegistration reg = context.registerService(ResourceAnalyzer.class.getName(), new WibbleAnalyzer(), analyzerProps);
		
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		StringWriter writer = new StringWriter();
		
		Set<File> files = new LinkedHashSet<File>();
		files.add(copyToTempFile(tempDir, "testdata/01-bsn+version.jar"));
		files.add(copyToTempFile(tempDir, "testdata/02-localization.jar"));
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(files, writer, config);
		
		assertEquals(readStream(TestOSGiServices.class.getResourceAsStream("/testdata/fragment-wibble-filtered.txt")), writer.toString().trim());
		
		context.ungetService(ref);
		reg.unregister();
	}
	
	// Test that exceptions thrown by analyzers are forwarded to the OSGi Log Service
	public void testLogNotification() throws Exception {
		// Register mock LogService, to receive notifications
		LogService mockLog = mock(LogService.class);
		ServiceRegistration mockLogReg = context.registerService(LogService.class.getName(), mockLog, null);
		
		// Register a broken analyzer that throws exceptions
		ResourceAnalyzer brokenAnalyzer = new ResourceAnalyzer() {
			public void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
				throw new Exception("Bang!");
			}
		};
		ServiceRegistration mockAnalyzerReg = context.registerService(ResourceAnalyzer.class.getName(), brokenAnalyzer, null);
		
		// Call the indexer
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		StringWriter writer = new StringWriter();
		Set<File> files = Collections.singleton(copyToTempFile(tempDir, "testdata/01-bsn+version.jar"));
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.ROOT_URL, tempDir.getAbsoluteFile().toURL().toString());
		indexer.indexFragment(files, writer, config);
		
		// Verify log output
		ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
		verify(mockLog).log(any(ServiceReference.class), eq(LogService.LOG_ERROR), anyString(), exceptionCaptor.capture());
		assertEquals("Bang!", exceptionCaptor.getValue().getMessage());

		mockAnalyzerReg.unregister();
		mockLogReg.unregister();
	}
	
}
