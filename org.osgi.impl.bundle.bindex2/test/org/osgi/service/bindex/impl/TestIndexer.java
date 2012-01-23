package org.osgi.service.bindex.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Collections;

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
	
}
