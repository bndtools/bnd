package org.osgi.service.bindex.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.bindex.ResourceIndexer;

import junit.framework.TestCase;

public class TestIndexer extends TestCase {

	public void testFragmentBsnVersion() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/01-bsn+version.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-01.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentLocalization() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/02-localization.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-02.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentExport() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/03-export.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-03.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentExportWithUses() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/04-export+uses.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-04.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentImport() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/05-import.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-05.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentRequireBundle() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/06-requirebundle.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-06.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentOptionalImport() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/07-optionalimport.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-07.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFullIndex() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		Set<File> files = new LinkedHashSet<File>();
		files.add(new File("testdata/03-export.jar"));
		files.add(new File("testdata/06-requirebundle.jar"));
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexerImpl.REPOSITORY_INCREMENT_OVERRIDE, "0");
		config.put(ResourceIndexer.REPOSITORY_NAME, "full-c+f");
		indexer.index(files, writer, config);
		
		String expected = Utils.readStream(new FileInputStream("testdata/full-03+06.txt"));
		assertEquals(expected, writer.toString());
	}
	
}
