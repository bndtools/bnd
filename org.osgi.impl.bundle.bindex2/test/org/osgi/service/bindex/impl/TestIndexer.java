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

	public void testFragmentA() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.a.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-a.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentB() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.b.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-b.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentC() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.c.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-c.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentD() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.d.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-d.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentE() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.e.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-e.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentF() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.f.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-f.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFragmentG() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/org.example.g.jar")), writer, null);
		
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-g.txt"));
		assertEquals(expected, writer.toString().trim());
	}
	
	public void testFullIndexCandF() throws Exception {
		ResourceIndexerImpl indexer = new ResourceIndexerImpl();
		
		StringWriter writer = new StringWriter();
		Set<File> files = new LinkedHashSet<File>();
		files.add(new File("testdata/org.example.c.jar"));
		files.add(new File("testdata/org.example.f.jar"));
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexerImpl.REPOSITORY_INCREMENT_OVERRIDE, "0");
		config.put(ResourceIndexer.REPOSITORY_NAME, "full-c+f");
		indexer.index(files, writer, config);
		
		String expected = Utils.readStream(new FileInputStream("testdata/full-c+f.txt"));
		assertEquals(expected, writer.toString());
	}
	
}
