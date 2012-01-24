package org.example.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.bindex.ResourceAnalyzer;
import org.osgi.service.bindex.ResourceIndexer;

public class TestBindex2 extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	public void testBasicServiceInvocation() throws Exception {
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/01-bsn+version.jar")), writer, null);
		
		assertEquals(Utils.readStream(new FileInputStream("testdata/fragment-basic.txt")), writer.toString().trim());
		
		context.ungetService(ref);
	}
	
	public void testWhiteboardAnalyzer() throws Exception {
		ServiceRegistration reg = context.registerService(ResourceAnalyzer.class.getName(), new WibbleAnalyzer(), null);
		
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/01-bsn+version.jar")), writer, null);
		
		assertEquals(Utils.readStream(new FileInputStream("testdata/fragment-wibble.txt")), writer.toString().trim());
		
		context.ungetService(ref);
		reg.unregister();
	}
	
	public void testWhiteboardAnalyzerWithFilter() throws Exception {
		Properties analyzerProps = new Properties();
		analyzerProps.put(ResourceAnalyzer.FILTER, "(location=*sion.jar)");
		ServiceRegistration reg = context.registerService(ResourceAnalyzer.class.getName(), new WibbleAnalyzer(), analyzerProps);
		
		ServiceReference ref = context.getServiceReference(ResourceIndexer.class.getName());
		ResourceIndexer indexer = (ResourceIndexer) context.getService(ref);
		StringWriter writer = new StringWriter();
		Set<File> files = new LinkedHashSet<File>();
		files.add(new File("testdata/01-bsn+version.jar"));
		files.add(new File("testdata/02-localization.jar"));
		indexer.indexFragment(files, writer, null);
		
		assertEquals(Utils.readStream(new FileInputStream("testdata/fragment-wibble-filtered.txt")), writer.toString().trim());
		
		context.ungetService(ref);
		reg.unregister();
	}}
